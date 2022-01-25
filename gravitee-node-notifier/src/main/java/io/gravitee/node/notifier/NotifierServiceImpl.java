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

import com.google.common.collect.Maps;
import io.gravitee.node.api.notifier.*;
import io.gravitee.node.notifier.plugin.NotifierPluginFactory;
import io.gravitee.node.notifier.trigger.NotificationTrigger;
import io.reactivex.Completable;
import io.vertx.core.Vertx;
import java.util.Map;
import java.util.Optional;
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
        definition.getType(),
        definition.getAudienceId()
      );
      triggers.put(id, notificationTrigger);
      notificationTrigger.start();
    } else {
      logger.debug("Node notifier service disabled");
    }
  }

  @Override
  public void unregister(String resourceId, String type, String audienceId) {
    final String id = buildNotificationId(resourceId, type, audienceId);
    Optional
      .ofNullable(triggers.get(id))
      .ifPresent(
        trigger -> {
          trigger.stop();
          triggers.remove(id);
        }
      );
  }

  @Override
  public Completable deleteAcknowledge(String resourceId) {
    return this.notificationAcknowledgeRepository.deleteByResourceId(
        resourceId
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
