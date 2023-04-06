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

import static org.mockito.Mockito.*;

import io.gravitee.node.api.notifier.NotificationAcknowledge;
import io.gravitee.node.api.notifier.NotificationAcknowledgeRepository;
import io.gravitee.node.api.notifier.NotificationCondition;
import io.gravitee.node.api.notifier.NotificationDefinition;
import io.gravitee.node.api.notifier.ResendNotificationCondition;
import io.gravitee.node.notifier.plugin.NotifierPluginFactory;
import io.gravitee.notifier.api.Notifier;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.Vertx;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationTriggerTest {

    @Spy
    private NotificationDefinition definition = new NotificationDefinition();

    @Mock
    private NotificationCondition condition;

    @Mock
    private ResendNotificationCondition resendCondition;

    @Spy
    private Vertx vertx = Vertx.vertx();

    @Mock
    private NotificationAcknowledgeRepository notificationAcknowledgeRepository;

    @Mock
    private NotifierPluginFactory notifierFactory;

    @Mock
    private Notifier notifier;

    @Before
    public void setup() {
        Mockito.reset(vertx);
    }

    @Test
    public void testShouldScheludeIfConditionNotTrue() {
        definition.setCron("*/10 * * * * *");

        NotificationTrigger cut = new NotificationTrigger(
            vertx,
            notificationAcknowledgeRepository,
            notifierFactory,
            definition,
            condition,
            resendCondition,
            true
        );

        when(condition.test(any())).thenReturn(false);

        cut.start();
        cut.handle(1l);

        verify(notificationAcknowledgeRepository, never()).findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any());
        verify(notificationAcknowledgeRepository, never()).create(any());
        verify(vertx, times(2)).setTimer(anyLong(), any());
        cut.stop();
    }

    @Test
    public void testShouldNotify() throws Exception {
        definition.setCron("*/10 * * * * *");
        definition.setResourceId("notifid");
        definition.setAudienceId("audid");
        definition.setType("email");

        NotificationTrigger cut = new NotificationTrigger(
            vertx,
            notificationAcknowledgeRepository,
            notifierFactory,
            definition,
            condition,
            resendCondition,
            true
        );

        when(condition.test(any())).thenReturn(true);
        when(notificationAcknowledgeRepository.findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any()))
            .thenReturn(Maybe.empty(), Maybe.just(new NotificationAcknowledge()));
        when(notifierFactory.create(any())).thenReturn(Optional.of(notifier));
        when(notifier.send(any(), any())).thenReturn(CompletableFuture.allOf());
        when(notificationAcknowledgeRepository.create(any())).thenReturn(Single.just(new NotificationAcknowledge()));

        cut.start();
        cut.handle(1l);

        Thread.sleep(2000);

        verify(notificationAcknowledgeRepository, atLeast(1)).findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any());
        verify(notificationAcknowledgeRepository)
            .create(argThat(na -> na.getResourceId().equals("notifid") && na.getType().equals("email")));
        verify(vertx, times(2)).setTimer(anyLong(), any());

        cut.stop();
    }

    @Test
    public void testShouldNotify_onlyOnce_when_stopped() throws Exception {
        definition.setCron("*/10 * * * * *");
        definition.setResourceId("notifid");
        definition.setAudienceId("audid");
        definition.setType("email");

        NotificationTrigger cut = new NotificationTrigger(
            vertx,
            notificationAcknowledgeRepository,
            notifierFactory,
            definition,
            condition,
            resendCondition,
            true
        );

        when(condition.test(any())).thenReturn(true);
        when(notificationAcknowledgeRepository.findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any()))
            .thenReturn(Maybe.empty(), Maybe.just(new NotificationAcknowledge()));
        when(notifierFactory.create(any())).thenReturn(Optional.of(notifier));
        when(notifier.send(any(), any())).thenReturn(CompletableFuture.allOf());
        when(notificationAcknowledgeRepository.create(any())).thenReturn(Single.just(new NotificationAcknowledge()));

        cut.start();
        cut.stop();
        cut.handle(1l);

        Thread.sleep(2000);

        verify(notificationAcknowledgeRepository, atLeast(1)).findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any());
        verify(notificationAcknowledgeRepository)
            .create(argThat(na -> na.getResourceId().equals("notifid") && na.getType().equals("email")));
        verify(vertx, times(1)).setTimer(anyLong(), any());
    }

    @Test
    public void testShouldNotNotify_IfAlreadyAcknowledged() throws Exception {
        definition.setCron("*/10 * * * * *");
        definition.setResourceId("notifid");
        definition.setAudienceId("audid");
        definition.setType("email");

        NotificationTrigger cut = new NotificationTrigger(
            vertx,
            notificationAcknowledgeRepository,
            notifierFactory,
            definition,
            condition,
            resendCondition,
            true
        );

        when(condition.test(any())).thenReturn(true);
        when(resendCondition.apply(any(), any())).thenReturn(false);
        when(notificationAcknowledgeRepository.findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any()))
            .thenReturn(Maybe.just(new NotificationAcknowledge()));
        cut.start();
        cut.handle(1l);

        Thread.sleep(2000);

        verify(notificationAcknowledgeRepository, atLeast(1)).findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any());
        verify(notificationAcknowledgeRepository, never()).create(any());
        verify(notificationAcknowledgeRepository, never()).update(any());
        cut.stop();
    }

    @Test
    public void testShouldResendNotification() throws Exception {
        definition.setCron("*/10 * * * * *");
        definition.setResourceId("notifid");
        definition.setAudienceId("audid");
        definition.setType("email");

        NotificationTrigger cut = new NotificationTrigger(
            vertx,
            notificationAcknowledgeRepository,
            notifierFactory,
            definition,
            condition,
            resendCondition,
            true
        );

        when(condition.test(any())).thenReturn(true);
        when(resendCondition.apply(any(), any())).thenReturn(true);
        final NotificationAcknowledge notificationAcknowledge = new NotificationAcknowledge();
        notificationAcknowledge.setCreatedAt(new Date());
        notificationAcknowledge.setType(definition.getType());
        notificationAcknowledge.setAudienceId(definition.getAudienceId());
        notificationAcknowledge.setResourceId(definition.getResourceId());
        notificationAcknowledge.setResourceType(definition.getResourceType());

        when(notificationAcknowledgeRepository.findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any()))
            .thenReturn(Maybe.just(notificationAcknowledge));
        when(notifierFactory.create(any())).thenReturn(Optional.of(notifier));
        when(notifier.send(any(), any())).thenReturn(CompletableFuture.allOf());

        cut.start();
        cut.handle(1l);

        Thread.sleep(2000);

        verify(notificationAcknowledgeRepository, atLeast(1)).findByResourceIdAndTypeAndAudienceId(any(), any(), any(), any());
        verify(notificationAcknowledgeRepository, never()).create(any());
        verify(notificationAcknowledgeRepository)
            .update(argThat(na -> na.getResourceId().equals("notifid") && na.getType().equals("email")));
        cut.stop();
    }
}
