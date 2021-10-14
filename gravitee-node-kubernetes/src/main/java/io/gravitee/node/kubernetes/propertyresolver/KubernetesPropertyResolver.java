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

import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.model.v1.ConfigMapEvent;
import io.gravitee.kubernetes.client.model.v1.KubernetesEventType;
import io.gravitee.kubernetes.client.model.v1.SecretEvent;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
public class KubernetesPropertyResolver implements PropertyResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    KubernetesPropertyResolver.class
  );

  @Autowired
  private KubernetesClient kubernetesClient;

  @Override
  public boolean supports(String currentValue) {
    Assert.notNull(currentValue, "Current value can not be null");

    return currentValue.startsWith(CloudScheme.KUBE.value());
  }

  @Override
  public Maybe<Object> resolve(String propertyName, String currentValue) {
    Assert.notNull(propertyName, "Property name can not be null");
    Assert.notNull(currentValue, "Current value can not be null");

    String[] properties = parsePropertyName(propertyName, currentValue); // kube://default/configmap/gravitee-config
    if (properties == null) {
      return Maybe.empty();
    }

    LOGGER.debug(
      "Resolve property [{}] in namespace [{}] using resource [{}]",
      propertyName,
      properties[0], // namespace
      properties[2] // resourceName
    );

    if ("secret".equals(properties[1])) { // type
      return resolvePropertyFromSecret(generateLocation(properties))
        .map(encodeData -> Base64.getDecoder().decode(encodeData));
    } else if ("configmap".equals(properties[1])) {
      return resolvePropertyFromConfigMap(generateLocation(properties))
        .map(String::strip);
    } else {
      LOGGER.warn("Property type [{}] is not supported", currentValue);
    }

    return Maybe.empty();
  }

  @Override
  public Flowable<Object> watch(String propertyName, String currentValue) {
    Assert.notNull(propertyName, "Property name can not be null");
    Assert.notNull(currentValue, "Current value can not be null");

    String[] properties = parsePropertyName(propertyName, currentValue); // kube://default/configmap/gravitee-config
    if (properties == null) {
      return Flowable.empty();
    }

    LOGGER.debug(
      "Start watching property [{}] in namespace [{}] using resource [{}]",
      propertyName,
      properties[0], // namespace
      properties[2] // resourceName
    );

    if ("secret".equals(properties[1])) { // type
      return kubernetesClient
        .watch(generateLocation(properties), SecretEvent.class)
        .filter(
          event ->
            event.getType().equals(KubernetesEventType.MODIFIED.name()) ||
            event.getType().equals(KubernetesEventType.ADDED.name())
        )
        .map(
          secretEvent -> {
            String encodedData = secretEvent
              .getObject()
              .getData()
              .get(properties[3]);
            return Base64.getDecoder().decode(encodedData);
          }
        );
    } else if ("configmap".equals(properties[1])) {
      return kubernetesClient
        .watch(generateLocation(properties), ConfigMapEvent.class)
        .filter(
          event ->
            event.getType().equals(KubernetesEventType.MODIFIED.name()) ||
            event.getType().equals(KubernetesEventType.ADDED.name())
        )
        .map(
          configMapEvent ->
            configMapEvent.getObject().getData().get(properties[3])
        );
    } else {
      LOGGER.warn("Property type [{}] is not supported", properties[1]);
      return Flowable.empty();
    }
  }

  private String[] parsePropertyName(String propertyName, String currentValue) {
    if (!supports(currentValue)) {
      LOGGER.error("Does not support scheme {}", currentValue);
      return null;
    }

    String[] properties = currentValue.substring(7).split("/"); // eliminate initial kube://

    if (properties.length < 3 || properties.length > 4) {
      LOGGER.error(
        "Wrong property value. A correct format looks like this \"kube://{namespace}/configmap/{configmap-name}\""
      );
      return null;
    } else if (properties.length == 3) {
      return new String[] {
        properties[0],
        properties[1],
        properties[2],
        propertyName,
      };
    }

    return properties;
  }

  private String generateLocation(String[] properties) {
    return String.format(
      "%s%s/%s/%s/%s",
      CloudScheme.KUBE.value(),
      properties[0],
      properties[1],
      properties[2],
      properties[3]
    );
  }

  private Maybe<String> resolvePropertyFromConfigMap(String location) {
    return kubernetesClient
      .get(location, String.class)
      .flatMap(
        data -> {
          if (data != null) {
            return Maybe.just(data);
          } else {
            LOGGER.warn("Key not found in this location [{}]", location);
            return Maybe.empty();
          }
        }
      );
  }

  private Maybe<String> resolvePropertyFromSecret(String location) {
    return kubernetesClient
      .get(location, String.class)
      .flatMap(
        data -> {
          if (data != null) {
            return Maybe.just(data);
          } else {
            LOGGER.debug("Key not found in this location [{}]", location);
            return Maybe.empty();
          }
        }
      );
  }
}
