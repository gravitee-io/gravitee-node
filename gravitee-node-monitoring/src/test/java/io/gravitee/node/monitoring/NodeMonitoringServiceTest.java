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
package io.gravitee.node.monitoring;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class NodeMonitoringServiceTest {

    @Mock
    private NodeMonitoringRepository repository;

    private NodeMonitoringService cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DefaultNodeMonitoringService(repository);
    }

    @Test
    void should_create() throws InterruptedException {
        final Monitoring monitoring = new Monitoring();
        monitoring.setNodeId("node#1");
        monitoring.setCreatedAt(new Date());
        monitoring.setEvaluatedAt(new Date());
        monitoring.setPayload("payload health check");
        monitoring.setType(Monitoring.HEALTH_CHECK);

        when(repository.findByNodeIdAndType("node#1", Monitoring.HEALTH_CHECK)).thenReturn(Maybe.empty());
        when(repository.create(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.await();
        obs.assertValue(monitoring);
    }

    @Test
    void should_update() throws InterruptedException {
        Monitoring monitoring = new Monitoring();
        monitoring.setNodeId("node#1");
        monitoring.setCreatedAt(new Date());
        monitoring.setEvaluatedAt(new Date());
        monitoring.setPayload("payload health check");
        monitoring.setType(Monitoring.HEALTH_CHECK);

        when(repository.findByNodeIdAndType("node#1", Monitoring.HEALTH_CHECK)).thenReturn(Maybe.empty());
        when(repository.create(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        monitoring = cut.createOrUpdate(monitoring).blockingGet();

        when(repository.update(monitoring)).thenAnswer(i -> Single.just(i.getArgument(0)));

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.await();
        obs.assertValue(monitoring);
    }

    @Test
    void should_not_create_or_update_if_not_repository() throws InterruptedException {
        final Monitoring monitoring = new Monitoring();
        cut = new DefaultNodeMonitoringService(null);

        final TestObserver<Monitoring> obs = cut.createOrUpdate(monitoring).test();

        obs.await();
        obs.assertValue(monitoring);

        verifyNoInteractions(repository);
    }

    @Test
    void should_find_by_type_and_timeframe() throws InterruptedException {
        long from = System.currentTimeMillis();
        long to = System.currentTimeMillis() + 1000;

        final Monitoring monitoring = new Monitoring();
        when(repository.findByTypeAndTimeFrame(Monitoring.HEALTH_CHECK, from, to)).thenReturn(Flowable.just(monitoring));

        final TestSubscriber<Monitoring> obs = cut.findByTypeAndTimeframe(Monitoring.HEALTH_CHECK, from, to).test();

        obs.await();
        obs.assertValue(monitoring);
        obs.assertComplete();
    }

    @Test
    void should_not_find_by_type_and_timeframe_if_no_Repository() throws InterruptedException {
        long from = System.currentTimeMillis();
        long to = System.currentTimeMillis() + 1000;

        cut = new DefaultNodeMonitoringService(null);
        final TestSubscriber<Monitoring> obs = cut.findByTypeAndTimeframe(Monitoring.HEALTH_CHECK, from, to).test();

        obs.await();
        obs.assertNoValues();
        obs.assertComplete();
    }
}
