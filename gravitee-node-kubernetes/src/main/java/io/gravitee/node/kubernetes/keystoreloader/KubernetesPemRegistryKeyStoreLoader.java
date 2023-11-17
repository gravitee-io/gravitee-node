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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.LabelSelector;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.kubernetes.client.api.WatchQuery;
import io.gravitee.kubernetes.client.config.KubernetesConfig;
import io.gravitee.kubernetes.client.model.v1.ConfigMap;
import io.gravitee.kubernetes.client.model.v1.ConfigMapList;
import io.gravitee.kubernetes.client.model.v1.Event;
import io.gravitee.kubernetes.client.model.v1.Secret;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KubernetesPemRegistryKeyStoreLoader extends AbstractKubernetesKeyStoreLoader<ConfigMap> {

    public static final String GRAVITEEIO_PEM_REGISTRY_LABEL = "gravitee.io/component";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KubernetesPemRegistryKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        super(options, kubernetesClient);
        prepareLocations();
    }

    private void prepareLocations() {
        if (options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM_REGISTRY)) {
            this.resources.put(CERTIFICATE_FORMAT_PEM_REGISTRY, null);
        }
    }

    public static boolean canHandle(KeyStoreLoaderOptions options) {
        return options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM_REGISTRY);
    }

    @Override
    protected Completable init() {
        return Flowable
            .fromIterable(resources.keySet())
            .flatMapCompletable(location -> {
                if (CERTIFICATE_FORMAT_PEM_REGISTRY.equals(location)) {
                    String currentNamespace = KubernetesConfig.getInstance().getCurrentNamespace();
                    return this.kubernetesClient.get(
                            ResourceQuery
                                .configMaps(currentNamespace)
                                .labelSelector(
                                    LabelSelector.equals(GRAVITEEIO_PEM_REGISTRY_LABEL, CERTIFICATE_FORMAT_PEM_REGISTRY.toLowerCase())
                                )
                                .build()
                        )
                        .flatMapCompletable(configMapList -> {
                            List<ConfigMap> items = configMapList.getItems();
                            if (items.isEmpty()) {
                                return Completable.error(new RuntimeException("No pem registry found in the current namespace"));
                            } else if (items.size() > 1) {
                                return Completable.error(new RuntimeException("multiple pem registry is not supported"));
                            } else {
                                String newLocation = String.format(
                                    "/%s/configmaps/%s",
                                    currentNamespace,
                                    items.get(0).getMetadata().getName()
                                );
                                this.resources.put(newLocation, ResourceQuery.<ConfigMap>from(newLocation).build());

                                return this.loadKeyStore(items.get(0));
                            }
                        })
                        .doOnComplete(() -> resources.remove(CERTIFICATE_FORMAT_PEM_REGISTRY));
                }

                return Completable.error(new IllegalArgumentException(String.format("unsupported keystore locations %s", location)));
            })
            .andThen(Completable.fromRunnable(this::emitKeyStoreEvent));
    }

    @Override
    protected Flowable<ConfigMap> watch() {
        return Flowable
            .fromIterable(resources.keySet())
            .flatMap(location ->
                kubernetesClient
                    .watch(WatchQuery.<ConfigMap>from(location).build())
                    .observeOn(Schedulers.computation())
                    .repeat()
                    .retryWhen(errors -> errors.delay(RETRY_DELAY_MILLIS, TimeUnit.MILLISECONDS))
            )
            .filter(event -> event.getType().equalsIgnoreCase("MODIFIED"))
            .map(Event::getObject);
    }

    @Override
    protected Completable loadKeyStore(ConfigMap configMap) {
        if (options.getType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM_REGISTRY)) {
            if (
                configMap.getMetadata().getLabels() != null &&
                CERTIFICATE_FORMAT_PEM_REGISTRY.equalsIgnoreCase(configMap.getMetadata().getLabels().get(GRAVITEEIO_PEM_REGISTRY_LABEL)) &&
                configMap.getData() != null
            ) {
                return generateKeystoreFromPemRegistry(configMap);
            } else {
                // make sure there is no keys left from the past
                keyStoresByLocation.put(GRAVITEEIO_PEM_REGISTRY_LABEL, initKeyStore());
                return Completable.complete();
            }
        }

        return Completable.error(new RuntimeException(String.format("unsupported keystore type %s", options.getType())));
    }

    private Completable generateKeystoreFromPemRegistry(ConfigMap configMap) {
        return Flowable
            .fromIterable(configMap.getData().values())
            .map(objectMapper::readTree)
            .filter(JsonNode::isArray)
            .flatMap(Flowable::fromIterable)
            .map(JsonNode::asText)
            .distinct()
            .filter(StringUtils::hasLength)
            .flatMapMaybe(reference -> {
                String[] namespaceName = reference.split("/");
                if (namespaceName.length != 2) {
                    return Maybe.error(new IllegalArgumentException("Wrong or missing namespace or name of the TLS Secret"));
                }
                return kubernetesClient.get(ResourceQuery.secret(namespaceName[0], namespaceName[1]).build()).map(this::secretToKeyStore);
            })
            .toList()
            .map(keyStoreList -> KeyStoreUtils.merge(keyStoreList, getPassword()))
            .doOnSuccess(keyStore -> keyStoresByLocation.put(GRAVITEEIO_PEM_REGISTRY_LABEL, keyStore))
            .ignoreElement();
    }

    private KeyStore initKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStoreUtils.DEFAULT_KEYSTORE_TYPE);
            keyStore.load(null, KeyStoreUtils.passwordToCharArray(getPassword()));
            return keyStore;
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to reset the %s keystore", GRAVITEEIO_PEM_REGISTRY_LABEL), e);
        }
    }

    private KeyStore secretToKeyStore(Secret secret) {
        final Map<String, String> data = secret.getData();

        if (data == null || data.isEmpty()) {
            throw new IllegalStateException(String.format("No data has been found in the secret %s", secret.getMetadata().getName()));
        }

        return KeyStoreUtils.initFromPem(
            new String(Base64.getDecoder().decode(data.get(KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_CRT)), StandardCharsets.UTF_8),
            new String(Base64.getDecoder().decode(data.get(KubernetesSecretKeyStoreLoader.KUBERNETES_TLS_KEY)), StandardCharsets.UTF_8),
            getPassword(),
            secret.getMetadata().getName()
        );
    }
}
