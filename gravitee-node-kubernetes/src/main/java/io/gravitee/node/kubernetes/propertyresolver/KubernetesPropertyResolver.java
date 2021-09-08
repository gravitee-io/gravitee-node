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
package io.gravitee.node.kubernetes.propertyresolver;

import io.gravitee.gateway.services.kube.client.KubernetesClient;
import io.gravitee.gateway.services.kube.client.KubernetesConfigMapV1Watcher;
import io.gravitee.gateway.services.kube.client.KubernetesSecretV1Watcher;
import io.gravitee.gateway.services.kube.client.model.v1.KubernetesEventType;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public class KubernetesPropertyResolver implements PropertyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesPropertyResolver.class);

    @Autowired private KubernetesClient kubernetesClient;
    @Autowired private KubernetesConfigMapV1Watcher kubernetesConfigMapV1Watcher;
    @Autowired private KubernetesSecretV1Watcher kubernetesSecretV1Watcher;

    private final Set<String> secretSet = new HashSet<>();
    private final Set<String> configMapSet = new HashSet<>();
    private Observable<Object> secretWatcher = null;
    private Observable<Object> configMapWatcher = null;
    Observable<Object> observable = Observable.create(emitter -> {});

    @Override
    public boolean supports(String propertyName) {
        Assert.notNull(propertyName, "Property name can not be null");

        return propertyName.startsWith(CloudScheme.KUBE.value());
    }

    @Override
    public Maybe<Object> resolve(String propertyName, String currentValue) {
        Assert.notNull(propertyName, "Property name can not be null");
        Assert.notNull(currentValue, "Current value can not be null");

        if (!supports(currentValue)) {
            LOGGER.debug("Does not support scheme {}", currentValue);
            return null;
        }

        KubeProperty kubeProperty = parsePropertyName(propertyName, currentValue); // kube://default/configmap/gravitee-config
        if (kubeProperty == null) {
            return null;
        }

        LOGGER.debug("Resolve property [{}] in namespace [{}] using resource [{}]", propertyName, kubeProperty.namespace, kubeProperty.resourceName);
        if (PropertyType.SECRET == kubeProperty.resourceType) {
            return getSecret(kubeProperty);
        } else if (PropertyType.CONFIGMAP == kubeProperty.resourceType) {
            return getConfigMap(kubeProperty);
        } else {
            LOGGER.warn("Property type [{}] is not supported", kubeProperty.resourceType.value);
        }

        return Maybe.empty();
    }


    @Override
    public Observable<Object> watch(String propertyName, String currentValue) {
        Assert.notNull(propertyName, "Property name can not be null");
        Assert.notNull(currentValue, "Current value can not be null");


        if (!supports(currentValue)) {
            LOGGER.debug("Does not support scheme {}", currentValue);
            return Observable.empty();
        }

        KubeProperty kubeProperty = parsePropertyName(propertyName, currentValue); // kube://default/configmap/gravitee-config
        if (kubeProperty == null) {
            return Observable.empty();
        }

        LOGGER.debug("Start watching property [{}] in namespace [{}] using resource [{}]", propertyName, kubeProperty.namespace, kubeProperty.resourceName);

        if (PropertyType.SECRET == kubeProperty.resourceType) {
            secretSet.add(kubeProperty.resourceName); // secret name
            if (secretWatcher == null) {
                secretWatcher = kubernetesSecretV1Watcher.watch(kubeProperty.namespace) // start watching the whole namespace to avoid creating many connections
                        .filter(event -> event.getType().equals(KubernetesEventType.MODIFIED.name()) || event.getType().equals(KubernetesEventType.ADDED.name()))
                        .filter(event -> secretSet.contains(JsonObject.mapFrom(event.getObject()).getJsonObject("metadata").getString("name")))
                        .flatMap(event -> getSecret(kubeProperty).toObservable());

                observable = observable.mergeWith(secretWatcher);
            }
        } else if (PropertyType.CONFIGMAP == kubeProperty.resourceType) {
            configMapSet.add(kubeProperty.resourceName); // configmap name
            if (configMapWatcher == null) {
                configMapWatcher = kubernetesConfigMapV1Watcher.watch(kubeProperty.namespace) // start watching the whole namespace to avoid creating many connections
                        .filter(event -> event.getType().equals(KubernetesEventType.MODIFIED.name()) || event.getType().equals(KubernetesEventType.ADDED.name()))
                        .filter(event -> configMapSet.contains(JsonObject.mapFrom(event.getObject()).getJsonObject("metadata").getString("name")))
                        .flatMap(event -> getConfigMap(kubeProperty).toObservable());

                observable = observable.mergeWith(configMapWatcher);
            }
        } else {
            LOGGER.warn("Property type [{}] is not supported", kubeProperty.resourceType.value);
        }

        return observable;
    }


    private KubeProperty parsePropertyName(String propertyName, String currentValue) {
        String[] properties = currentValue.substring(7).split("/"); // eliminate initial kube://

        if (properties.length < 3) {
            LOGGER.error("Wrong property value. A correct format looks like this \"kube://{namespace}/configmap/{configmap-name}\"");
            return null;
        }

        String key = properties.length == 4 ? properties[3] : propertyName;
        PropertyType propertyType = null;
        for (PropertyType type : PropertyType.values()) {
            if (type.value().equals(properties[1])) {
                propertyType = type;
            }
        }
        return new KubeProperty(properties[0], propertyType, properties[2], key);
    }

    private Maybe<Object> getConfigMap(KubeProperty kubeProperty) {
        return kubernetesClient.configMap(kubeProperty.namespace, kubeProperty.resourceName).flatMap(configMap -> {
            String value = configMap.getData().get(kubeProperty.key);
            if (!StringUtils.isEmpty(value)) {
                return Maybe.just(value);
            } else {
                LOGGER.debug("Key [{}] not found in configmap [{}]", kubeProperty.key, kubeProperty.resourceName);
                return Maybe.empty();
            }
        });
    }

    private Maybe<Object> getSecret(KubeProperty kubeProperty) {
        return kubernetesClient.secret(kubeProperty.namespace, kubeProperty.resourceName).flatMap(secret -> {
            String encodedValue = secret.getData().get(kubeProperty.key);
            if (!StringUtils.isEmpty(encodedValue)) {
                return Maybe.just(new String(Base64.getDecoder().decode(encodedValue)));
            } else {
                LOGGER.debug("Key [{}] not found in secret [{}]", kubeProperty.key, kubeProperty.resourceName);
                return Maybe.empty();
            }
        });
    }

    private static class KubeProperty {
        private final String namespace;
        private final PropertyType resourceType;
        private final String resourceName;
        private final String key;

        private KubeProperty(String namespace, PropertyType resourceType, String resourceName, String key) {
            this.namespace = namespace;
            this.resourceType = resourceType;
            this.resourceName = resourceName;
            this.key = key;
        }
    }

    private enum PropertyType {

        SECRET("secret"),
        CONFIGMAP("configmap");

        private final String value;

        PropertyType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

}
