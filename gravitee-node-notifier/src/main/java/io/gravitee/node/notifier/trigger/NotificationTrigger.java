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
package io.gravitee.node.notifier.trigger;

import io.gravitee.node.api.notifier.*;
import io.gravitee.node.notifier.plugin.NotifierPluginFactory;
import io.gravitee.notifier.api.Notification;
import io.gravitee.notifier.api.Notifier;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationTrigger implements Handler<Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTrigger.class);

    private Vertx vertx;

    private NotificationAcknowledgeRepository notificationAcknowledgeRepository;

    private NotifierPluginFactory notifierFactory;

    private NotificationDefinition definition;

    private NotificationCondition condition;

    private ResendNotificationCondition resendCondition;

    private Long scheduledTaskId;

    private CronExpression cronExpression;

    private final int randomDelayInMs;

    public NotificationTrigger(
        Vertx vertx,
        NotificationAcknowledgeRepository notificationAcknowledgeRepository,
        NotifierPluginFactory notifierFactory,
        NotificationDefinition definition,
        NotificationCondition condition,
        ResendNotificationCondition resendCondition,
        boolean tryToAvoidMultipleNotif
    ) {
        this.vertx = vertx;
        this.notificationAcknowledgeRepository = notificationAcknowledgeRepository;
        this.notifierFactory = notifierFactory;
        this.definition = definition;
        this.condition = condition;
        this.resendCondition = resendCondition;
        this.cronExpression = CronExpression.parse(definition.getCron());
        if (tryToAvoidMultipleNotif) {
            this.randomDelayInMs = Math.max(1, new Random().nextInt(10)) * 1000;
        } else {
            this.randomDelayInMs = -1;
        }
    }

    public void start() {
        scheduleNextAttempt();
    }

    private void scheduleNextAttempt() {
        this.scheduledTaskId = this.vertx.setTimer(computeNextAttempt(), this);
    }

    public void stop() {
        if (this.scheduledTaskId != null && this.vertx.cancelTimer(this.scheduledTaskId)) {
            LOGGER.debug("Notification Trigger cancelled !");
        } else {
            LOGGER.debug("Notification Trigger can't be cancelled or doesn't exist");
        }
    }

    private long computeNextAttempt() {
        final LocalDateTime now = LocalDateTime.now();
        final long delay = toEpochMillis(this.cronExpression.next(now)) - toEpochMillis(now);
        return randomDelayInMs > 0 ? delay + randomDelayInMs : delay;
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    @Override
    public void handle(Long event) {
        if (condition.test(definition)) {
            // keep EventLoop context in order to use it to execute the notifier's send method
            final Context triggerContext = vertx.getOrCreateContext();
            this.notificationAcknowledgeRepository.findByResourceIdAndTypeAndAudienceId(
                    this.definition.getResourceId(),
                    this.definition.getResourceType(),
                    definition.getType(),
                    definition.getAudienceId()
                )
                .map(Optional::ofNullable)
                .switchIfEmpty(Maybe.just(Optional.empty()))
                .subscribe(
                    notifAck -> {
                        final boolean firstNotification = notifAck.isEmpty();
                        if (firstNotification || resendCondition.apply(definition, notifAck.get())) {
                            // instantiate the plugin and send notification
                            final Optional<Notifier> optNotifier = notifierFactory.create(definition);
                            if (optNotifier.isPresent()) {
                                LOGGER.debug(
                                    "Send Notification with notifier type {} to audience {} about resource {}",
                                    definition.getType(),
                                    definition.getAudienceId(),
                                    definition.getResourceId()
                                );
                                final Notifier notifier = optNotifier.get();
                                // execute the send method into the triggerContext to avoid NullPointer as some notifiers rely on Vertx.currentContext
                                // that will be null as this block of code is executed in a RxJava ThreadPool and not into the Vertx EventLoop
                                triggerContext.runOnContext(aVoid ->
                                    notifier
                                        .send(buildNotification(definition), definition.getData())
                                        .whenComplete((Void anotherVoid, Throwable throwable) -> {
                                            if (throwable != null) {
                                                LOGGER.error(
                                                    "An error occurs while sending notification to {}",
                                                    definition.getType(),
                                                    throwable
                                                );
                                                // trigger a new attempt
                                                this.scheduleNextAttempt();
                                            } else {
                                                LOGGER.debug("A notification has been sent to {}", definition.getType());

                                                NotificationAcknowledge notificationAcknowledge = notifAck.orElse(
                                                    new NotificationAcknowledge()
                                                );
                                                Date now = new Date();
                                                notificationAcknowledge.setUpdatedAt(now);
                                                if (firstNotification) {
                                                    // Acknowledge has just been created
                                                    notificationAcknowledge.setCreatedAt(now);
                                                    notificationAcknowledge.setType(definition.getType());
                                                    notificationAcknowledge.setAudienceId(definition.getAudienceId());
                                                    notificationAcknowledge.setResourceId(definition.getResourceId());
                                                    notificationAcknowledge.setResourceType(definition.getResourceType());
                                                } else {
                                                    // existing acknowledge, increment the number of notifications
                                                    notificationAcknowledge.incrementCounter();
                                                }

                                                final Single<NotificationAcknowledge> saveAcknowledge = firstNotification
                                                    ? notificationAcknowledgeRepository.create(notificationAcknowledge)
                                                    : notificationAcknowledgeRepository.update(notificationAcknowledge);

                                                saveAcknowledge
                                                    .doOnError(error ->
                                                        LOGGER.warn(
                                                            "Unable to store acknowledge for notification with audience {} and resource {}",
                                                            definition.getAudienceId(),
                                                            definition.getResourceId(),
                                                            error
                                                        )
                                                    )
                                                    .doFinally(() ->
                                                        //trigger a new attempt
                                                        this.scheduleNextAttempt()
                                                    )
                                                    .subscribe();
                                            }
                                        })
                                );
                            } else {
                                LOGGER.warn(
                                    "Notifier {} not found, unable to send notification to target {} about resource {}",
                                    definition.getType(),
                                    definition.getAudienceId(),
                                    definition.getResourceId()
                                );
                            }
                        } else {
                            LOGGER.debug(
                                "Notification about resource {} already sent to target {}",
                                definition.getResourceId(),
                                definition.getAudienceId()
                            );
                            // trigger a new attempt.
                            // this will allow a new notification
                            // if DB entries are purged as we will have no
                            // way to know if the notification has been sent or not
                            this.scheduleNextAttempt();
                        }
                    },
                    error -> {
                        //trigger a new attempt
                        this.scheduleNextAttempt();
                        LOGGER.warn("Notification can't be send : {}", error.getMessage());
                    }
                );
        } else {
            // if rule isn't respected, trigger a new attempt
            this.scheduleNextAttempt();
        }
    }

    private final Notification buildNotification(NotificationDefinition definition) {
        Notification notification = new Notification();
        notification.setConfiguration(definition.getConfiguration());
        notification.setType(definition.getType());
        return notification;
    }
}
