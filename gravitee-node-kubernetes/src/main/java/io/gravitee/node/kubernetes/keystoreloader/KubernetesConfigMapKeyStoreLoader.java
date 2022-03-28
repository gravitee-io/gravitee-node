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
import io.gravitee.kubernetes.client.KubernetesClientV1Impl.KubernetesResource;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ConfigMapEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesConfigMapKeyStoreLoader extends AbstractKubernetesKeyStoreLoader<ConfigMap> {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12.toLowerCase()
    );

    private static final Pattern CONFIGMAP_PATTERN = Pattern.compile("^(.*/configmaps/.*)/.*$");

    public KubernetesConfigMapKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        super(options, kubernetesClient);
        prepareLocations();
    }

    private void prepareLocations() {
        this.options.getKubernetesLocations()
            .forEach(location -> {
                final Matcher matcher = CONFIGMAP_PATTERN.matcher(location);
                if (matcher.matches()) {
                    this.resources.put(matcher.group(1), new KubernetesResource(location));
                } else {
                    throw new IllegalArgumentException(
                        "You must specify a data when using configmap (ex: /my-namespace/configmaps/my-configmap/my-keystore)."
                    );
                }
            });
    }

    public static boolean canHandle(KeyStoreLoaderOptions options) {
        final List<String> kubernetesLocations = options.getKubernetesLocations();

        return (
            kubernetesLocations != null &&
            !kubernetesLocations.isEmpty() &&
            SUPPORTED_TYPES.contains(options.getKeyStoreType().toLowerCase()) &&
            kubernetesLocations.stream().allMatch(location -> CONFIGMAP_PATTERN.matcher(location).matches())
        );
    }

    @Override
    protected Completable init() {
        final List<Completable> locationObs = resources
            .keySet()
            .stream()
            .map(location ->
                kubernetesClient.get(location, ConfigMap.class).observeOn(Schedulers.computation()).flatMapCompletable(this::loadKeyStore)
            )
            .collect(Collectors.toList());

        return Completable
            .merge(locationObs)
            .observeOn(Schedulers.computation())
            .andThen(Completable.fromRunnable(this::refreshKeyStoreBundle));
    }

    @Override
    protected Flowable<ConfigMap> watch() {
        final List<Flowable<ConfigMapEvent>> toWatch = resources
            .keySet()
            .stream()
            .map(location ->
                kubernetesClient
                    .watch(location, ConfigMapEvent.class)
                    .observeOn(Schedulers.computation())
                    .repeat()
                    .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS))
            )
            .collect(Collectors.toList());

        return Flowable.merge(toWatch).filter(event -> event.getType().equalsIgnoreCase("MODIFIED")).map(ConfigMapEvent::getObject);
    }

    @Override
    protected Completable loadKeyStore(ConfigMap configMap) {
        final Optional<KubernetesResource> optResource = resources
            .values()
            .stream()
            .filter(r ->
                r.getNamespace().equalsIgnoreCase(configMap.getMetadata().getNamespace()) &&
                r.getName().equalsIgnoreCase(configMap.getMetadata().getName())
            )
            .findFirst();

        if (optResource.isEmpty()) {
            return Completable.error(new IllegalArgumentException("Unable to load keystore: unknown secret."));
        }

        Map<String, String> data = configMap.getBinaryData() == null ? configMap.getData() : configMap.getBinaryData();

        if (configMap.getBinaryData() != null) {
            data = configMap.getBinaryData();
        } else if (configMap.getData() != null) {
            data = configMap.getData();
        }

        final String dataKey = optResource.get().getKey();
        if (data == null || data.get(dataKey) == null) {
            return Completable.error(
                new IllegalArgumentException(String.format("No data has been found in the configmap for the specified key [%s].", dataKey))
            );
        }

        final KeyStore keyStore = KeyStoreUtils.initFromContent(
            options.getKeyStoreType(),
            data.get(dataKey),
            options.getKeyStorePassword()
        );

        keyStoresByLocation.put(configMap.getMetadata().getUid(), keyStore);
        return Completable.complete();
    }
}
