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
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesConfigMapKeyStoreLoader extends AbstractKubernetesKeyStoreLoader<ConfigMap> {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12.toLowerCase(),
        KeyStoreLoader.CERTIFICATE_FORMAT_PEM_REGISTRY.toLowerCase()
    );

    private static final Pattern CONFIGMAP_PATTERN = Pattern.compile("^(.*/configmaps/.*)/.*$");
    private static final Pattern PEM_REGISTRY_CONFIGMAP_PATTERN = Pattern.compile("^(.*/configmaps)/.*$");

    public KubernetesConfigMapKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        super(options, kubernetesClient);
        prepareLocations();
    }

    private void prepareLocations() {
        this.options.getKubernetesLocations()
            .forEach(location -> {
                final Matcher matcher = CONFIGMAP_PATTERN.matcher(location);
                final Matcher pemRegistryMatcher = PEM_REGISTRY_CONFIGMAP_PATTERN.matcher(location);
                if (matcher.matches()) {
                    this.resources.put(matcher.group(1), ResourceQuery.<ConfigMap>from(location).build());
                } else if (pemRegistryMatcher.matches()) {
                    this.resources.put(location, ResourceQuery.<ConfigMap>from(location).build());
                } else {
                    throw new IllegalArgumentException(
                        "You must specify a data when using configmap (ex: /my-namespace/configmaps/my-configmap/my-keystore or /my-namespace/configmaps/" +
                        GRAVITEEIO_PEM_REGISTRY +
                        ")."
                    );
                }
            });
    }

    public static boolean canHandle(KeyStoreLoaderOptions options) {
        final List<String> kubernetesLocations = options.getKubernetesLocations();

        return (
            kubernetesLocations != null &&
            !kubernetesLocations.isEmpty() &&
            SUPPORTED_TYPES.contains(options.getType().toLowerCase()) &&
            (
                kubernetesLocations
                    .stream()
                    .allMatch(location ->
                        CONFIGMAP_PATTERN.matcher(location).matches() || PEM_REGISTRY_CONFIGMAP_PATTERN.matcher(location).matches()
                    )
            )
        );
    }

    @Override
    protected Completable init() {
        final List<Completable> locationObs = resources
            .keySet()
            .stream()
            .map(location ->
                kubernetesClient
                    .get(ResourceQuery.<ConfigMap>from(location).build())
                    .observeOn(Schedulers.computation())
                    .flatMapCompletable(this::loadKeyStore)
            )
            .toList();

        return Completable
            .merge(locationObs)
            .observeOn(Schedulers.computation())
            .andThen(Completable.fromRunnable(this::emitKeyStoreEvent));
    }

    @Override
    protected Flowable<ConfigMap> watch() {
        final List<Flowable<Event<ConfigMap>>> toWatch = resources
            .keySet()
            .stream()
            .map(location ->
                kubernetesClient
                    .watch(WatchQuery.<ConfigMap>from(location).build())
                    .observeOn(Schedulers.computation())
                    .repeat()
                    .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS))
            )
            .toList();

        return Flowable.merge(toWatch).filter(event -> event.getType().equalsIgnoreCase("MODIFIED")).map(Event::getObject);
    }

    @Override
    protected Completable loadKeyStore(ConfigMap configMap) {
        KeyStore keyStore = null;
        if (this.options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM_REGISTRY)) {
            if (
                configMap.getMetadata().getAnnotations() != null &&
                "true".equals(configMap.getMetadata().getAnnotations().get(GRAVITEEIO_PEM_REGISTRY)) &&
                configMap.getData() != null
            ) {
                return generateKeystoreFromPemRegistry(configMap);
            } else {
                // make sure there is no keys left from the past
                keyStoresByLocation.put(GRAVITEEIO_PEM_REGISTRY, initKeyStore());
            }
        } else {
            final Optional<ResourceQuery<ConfigMap>> optResource = resources
                .values()
                .stream()
                .filter(r ->
                    r.getNamespace().equalsIgnoreCase(configMap.getMetadata().getNamespace()) &&
                    r.getResource().equalsIgnoreCase(configMap.getMetadata().getName())
                )
                .findFirst();

            if (optResource.isEmpty()) {
                return Completable.error(new IllegalArgumentException("Unable to load keystore: unknown configmap."));
            }

            Map<String, String> data = getConfigMapData(configMap);

            final String dataKey = optResource.get().getResourceKey();
            if (data == null || data.get(dataKey) == null) {
                return Completable.error(
                    new IllegalArgumentException(
                        String.format("No data has been found in the configmap for the specified key [%s].", dataKey)
                    )
                );
            }

            keyStore = KeyStoreUtils.initFromContent(this.options.getType(), data.get(dataKey), getPassword());
        }

        if (keyStore != null) {
            keyStoresByLocation.put(configMap.getMetadata().getUid(), keyStore);
        }
        return Completable.complete();
    }

    private static Map<String, String> getConfigMapData(ConfigMap configMap) {
        Map<String, String> data = configMap.getBinaryData() == null ? configMap.getData() : configMap.getBinaryData();

        if (configMap.getBinaryData() != null) {
            data = configMap.getBinaryData();
        } else if (configMap.getData() != null) {
            data = configMap.getData();
        }
        return data;
    }

    private Completable generateKeystoreFromPemRegistry(ConfigMap configMap) {
        return Maybe
            .merge(
                configMap
                    .getData()
                    .values()
                    .stream()
                    .map(name -> {
                        if (!StringUtils.hasLength(name)) {
                            return Maybe.<KeyStore>error(new IllegalArgumentException("Wrong or missing TLS secret name"));
                        }

                        return kubernetesClient
                            .get(ResourceQuery.secret(configMap.getMetadata().getNamespace(), name).build())
                            .map(this::secretToKeyStore);
                    })
                    .toList()
            )
            .toList()
            .map(keyStoreList -> KeyStoreUtils.merge(keyStoreList, getPassword()))
            .doOnSuccess(keyStore -> keyStoresByLocation.put(GRAVITEEIO_PEM_REGISTRY, keyStore))
            .ignoreElement();
    }

    private KeyStore initKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
            keyStore.load(null, KeyStoreUtils.passwordToCharArray(getPassword()));
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to reset the %s keystore", GRAVITEEIO_PEM_REGISTRY), e);
        }
    }

    private KeyStore secretToKeyStore(Secret secret) {
        final Map<String, String> data = secret.getData();

        if (data == null || data.isEmpty()) {
            throw new RuntimeException(String.format("No data has been found in the secret %s", secret.getMetadata().getName()));
        }

        return KeyStoreUtils.initFromPem(
            new String(Base64.getDecoder().decode(data.get(KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_CRT))),
            new String(Base64.getDecoder().decode(data.get(KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_KEY))),
            getPassword(),
            secret.getMetadata().getName()
        );
    }
}
