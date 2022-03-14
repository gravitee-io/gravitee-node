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
package io.gravitee.node.notifier;

import static io.gravitee.node.notifier.NotifierUtils.buildNotificationId;
import static io.gravitee.node.notifier.NotifierUtils.buildResourceKey;

import com.google.common.collect.Maps;
import io.gravitee.node.api.notifier.*;
import io.gravitee.node.notifier.plugin.NotifierPluginFactory;
import io.gravitee.node.notifier.trigger.NotificationTrigger;
import io.reactivex.Completable;
import io.vertx.core.Vertx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class NotifierServiceImpl implements NotifierService {

  private final Logger logger = LoggerFactory.getLogger(
    NotifierServiceImpl.class
  );

  @Value("${services.notifier.enabled:false}")
  private boolean enabled;

  @Value("${services.notifier.tryAvoidDuplicateNotification:false}")
  private boolean tryAvoidDuplicateNotification;

  @Autowired
  private Vertx vertx;

  @Autowired
  private NotifierPluginFactory notifierFactory;

  @Autowired
  @Lazy
  private NotificationAcknowledgeRepository notificationAcknowledgeRepository;

  private Map<String, NotificationTrigger> triggers = Maps.newConcurrentMap();
  private Map<String, List<NotificationDefinition>> definitions = Maps.newConcurrentMap();

  @Override
  public void register(
    NotificationDefinition definition,
    NotificationCondition condition,
    ResendNotificationCondition resendCondition
  ) {
    if (enabled) {
      final var notificationTrigger = getTrigger(
        definition,
        condition,
        resendCondition
      );
      final String id = buildNotificationId(
        definition.getResourceId(),
        definition.getResourceType(),
        definition.getType(),
        definition.getAudienceId()
      );
      triggers.put(id, notificationTrigger);
      preserveNotificationDefinition(definition);

      notificationTrigger.start();
    } else {
      logger.debug("Node notifier service disabled");
    }
  }

  private void preserveNotificationDefinition(
    NotificationDefinition definition
  ) {
    final String resourceKey = buildResourceKey(
      definition.getResourceId(),
      definition.getResourceType()
    );
    definitions
      .computeIfAbsent(
        resourceKey,
        key -> Collections.synchronizedList(new ArrayList<>())
      )
      .add(definition);
  }

  @Override
  public void unregisterAll(String resourceId, String resourceType) {
    final String resourceKey = buildResourceKey(resourceId, resourceType);
    List<NotificationDefinition> defs = definitions.get(resourceKey);
    if (defs != null) {
      defs.forEach(
        def -> {
          final String id = buildNotificationId(
            def.getResourceId(),
            def.getResourceType(),
            def.getType(),
            def.getAudienceId()
          );
          stopAndRemoveTrigger(id);
        }
      );
      definitions.remove(resourceKey);
    }
  }

  @Override
  public void unregister(
    String resourceId,
    String resourceType,
    String type,
    String audienceId
  ) {
    final String id = buildNotificationId(
      resourceId,
      resourceType,
      type,
      audienceId
    );
    stopAndRemoveTrigger(id);

    removeDefinition(resourceId, resourceType, type, audienceId);
  }

  private void stopAndRemoveTrigger(String id) {
    Optional
      .ofNullable(triggers.get(id))
      .ifPresent(
        trigger -> {
          trigger.stop();
          triggers.remove(id);
        }
      );
  }

  private void removeDefinition(
    String resourceId,
    String resourceType,
    String type,
    String audienceId
  ) {
    final NotificationDefinition definitionToDelete = new NotificationDefinition();
    definitionToDelete.setResourceType(resourceType);
    definitionToDelete.setResourceId(resourceId);
    definitionToDelete.setAudienceId(audienceId);
    definitionToDelete.setType(type);
    final String resourceKey = buildResourceKey(resourceId, resourceType);
    if (definitions.containsKey(resourceKey)) {
      definitions.get(resourceKey).remove(definitionToDelete);
    }
  }

  @Override
  public Completable deleteAcknowledge(String resourceId, String resourceType) {
    return this.notificationAcknowledgeRepository.deleteByResourceId(
        resourceId,
        resourceType
      );
  }

  private NotificationTrigger getTrigger(
    NotificationDefinition definition,
    NotificationCondition condition,
    ResendNotificationCondition resendCondition
  ) {
    NotificationTrigger trigger = new NotificationTrigger(
      vertx,
      notificationAcknowledgeRepository,
      notifierFactory,
      definition,
      condition,
      resendCondition,
      this.tryAvoidDuplicateNotification
    );
    return trigger;
  }
}
