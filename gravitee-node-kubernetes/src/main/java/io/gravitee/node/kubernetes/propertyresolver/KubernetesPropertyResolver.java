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

    return currentValue.startsWith(CloudScheme.KUBERNETES.value());
  }

  @Override
  public Maybe<Object> resolve(String location) {
    Assert.notNull(location, "Location can not be null");

    String[] properties = parsePropertyName(location); // kubernetes://default/configmaps/gravitee-config/management.db.name
    if (properties == null) {
      return Maybe.empty();
    }

    LOGGER.debug("Resolve location [{}]", location);

    if ("secrets".equals(properties[1])) { // type
      return resolvePropertyFromSecret(generateLocation(properties))
        .map(encodeData -> new String(Base64.getDecoder().decode(encodeData)));
    } else if ("configmaps".equals(properties[1])) {
      return resolvePropertyFromConfigMap(generateLocation(properties))
        .map(String::strip);
    } else {
      return Maybe.error(
        new RuntimeException(
          "Property type " + properties[1] + " is not supported"
        )
      );
    }
  }

  @Override
  public Flowable<Object> watch(String location) {
    Assert.notNull(location, "Location can not be null");

    String[] properties = parsePropertyName(location); // kubernetes://default/configmaps/gravitee-config/my_key
    if (properties == null) {
      return Flowable.empty();
    }

    LOGGER.debug("Start watching location [{}]", location);

    if ("secrets".equals(properties[1])) { // type
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
            return new String(Base64.getDecoder().decode(encodedData));
          }
        );
    } else if ("configmaps".equals(properties[1])) {
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
      return Flowable.error(
        new RuntimeException(
          "Property type " + properties[1] + " is not supported"
        )
      );
    }
  }

  private String[] parsePropertyName(String currentValue) {
    if (!supports(currentValue)) {
      LOGGER.error("Does not support scheme {}", currentValue);
      return null;
    }

    String[] properties = currentValue.substring(13).split("/"); // eliminate initial kubernetes://

    if (properties.length != 4) {
      LOGGER.error(
        "Wrong property value. A correct format looks like this \"kubernetes://{namespace}/configmaps/{configmap-name}/key\""
      );
      return null;
    }

    return properties;
  }

  private String generateLocation(String[] properties) {
    return String.format(
      "/%s/%s/%s/%s",
      properties[0], // namespace
      properties[1], // resource type
      properties[2], // name
      properties[3] // key
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
