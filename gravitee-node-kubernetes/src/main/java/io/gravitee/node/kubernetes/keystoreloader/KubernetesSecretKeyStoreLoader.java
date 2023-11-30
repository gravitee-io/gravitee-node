/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.kubernetes.keystoreloader;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesSecretKeyStoreLoader extends AbstractKubernetesKeyStoreLoader<Secret> {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PEM.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12.toLowerCase()
    );

    private static final Pattern SECRET_PATTERN = Pattern.compile("^(.*)/secrets/(.*)$");
    private static final Pattern SECRET_OPAQUE_PATTERN = Pattern.compile("^(.*/secrets/[^/]*)/.*$");
    protected static final String KUBERNETES_TLS_SECRET = "kubernetes.io/tls";
    protected static final String KUBERNETES_OPAQUE_SECRET = "Opaque";
    protected static final String KUBERNETES_TLS_CRT = "tls.crt";
    protected static final String KUBERNETES_TLS_KEY = "tls.key";

    public KubernetesSecretKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        super(options, kubernetesClient);
        prepareLocations();
    }

    private void prepareLocations() {
        this.options.getKubernetesLocations()
            .forEach(location -> {
                final Matcher matcher = SECRET_OPAQUE_PATTERN.matcher(location);
                if (matcher.matches()) {
                    this.resources.put(matcher.group(1), ResourceQuery.<Secret>from(location).build());
                } else {
                    this.resources.put(location, ResourceQuery.<Secret>from(location).build());
                }
            });
    }

    public static boolean canHandle(KeyStoreLoaderOptions options) {
        final List<String> kubernetesLocations = options.getKubernetesLocations();

        return (
            kubernetesLocations != null &&
            !kubernetesLocations.isEmpty() &&
            SUPPORTED_TYPES.contains(options.getKeyStoreType().toLowerCase()) &&
            kubernetesLocations.stream().allMatch(location -> SECRET_PATTERN.matcher(location).matches())
        );
    }

    @Override
    protected Completable init() {
        final List<Completable> locationObs = resources
            .keySet()
            .stream()
            .map(location -> kubernetesClient.get(ResourceQuery.<Secret>from(location).build()).flatMapCompletable(this::loadKeyStore))
            .collect(Collectors.toList());

        return Completable.merge(locationObs).andThen(Completable.fromRunnable(this::refreshKeyStoreBundle));
    }

    @Override
    protected Flowable<Secret> watch() {
        final List<Flowable<Event<Secret>>> toWatch = resources
            .keySet()
            .stream()
            .map(location ->
                kubernetesClient
                    .watch(WatchQuery.<Secret>from(location).build())
                    .observeOn(Schedulers.computation())
                    .repeat()
                    .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS))
            )
            .collect(Collectors.toList());

        return Flowable.merge(toWatch).filter(event -> event.getType().equalsIgnoreCase("MODIFIED")).map(Event::getObject);
    }

    @Override
    protected Completable loadKeyStore(Secret secret) {
        final Map<String, String> data = secret.getData();
        final KeyStore keyStore;

        if (secret.getType().equals(KUBERNETES_TLS_SECRET)) {
            keyStore =
                KeyStoreUtils.initFromPem(
                    new String(Base64.getDecoder().decode(data.get(KUBERNETES_TLS_CRT))),
                    new String(Base64.getDecoder().decode(data.get(KUBERNETES_TLS_KEY))),
                    options.getKeyStorePassword(),
                    secret.getMetadata().getName()
                );
        } else if (secret.getType().equals(KUBERNETES_OPAQUE_SECRET)) {
            if (options.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                return Completable.error(
                    new IllegalArgumentException("Pem format is not supported with opaque secret, use kubernetes tls secret instead.")
                );
            } else {
                final Optional<ResourceQuery<Secret>> optResource = resources
                    .values()
                    .stream()
                    .filter(r ->
                        r.getNamespace().equalsIgnoreCase(secret.getMetadata().getNamespace()) &&
                        (
                            secret.getType().equalsIgnoreCase(KUBERNETES_OPAQUE_SECRET) ||
                            r.getType().getName().equalsIgnoreCase(secret.getType())
                        ) &&
                        r.getResource().equalsIgnoreCase(secret.getMetadata().getName())
                    )
                    .findFirst();

                if (optResource.isEmpty()) {
                    return Completable.error(new IllegalArgumentException("Unable to load keystore: unknown secret."));
                } else if (optResource.get().getResourceKey() == null || optResource.get().getResourceKey().isEmpty()) {
                    return Completable.error(
                        new IllegalArgumentException(
                            "You must specify a data when using opaque secret (ex: /my-namespace/secrets/my-secret/my-keystore)."
                        )
                    );
                }

                keyStore =
                    KeyStoreUtils.initFromContent(
                        options.getKeyStoreType(),
                        data.get(optResource.get().getResourceKey()),
                        options.getKeyStorePassword()
                    );
            }
        } else {
            return Completable.error(new IllegalArgumentException(String.format("Invalid secret type [%s]", secret.getType())));
        }

        keyStoresByLocation.put(secret.getMetadata().getUid(), keyStore);
        return Completable.complete();
    }
}
