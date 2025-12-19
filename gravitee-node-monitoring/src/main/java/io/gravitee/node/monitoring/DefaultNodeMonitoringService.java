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

import io.gravitee.common.utils.UUID;
import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class DefaultNodeMonitoringService implements NodeMonitoringService {

    private static final int CLEANUP_DELAY = 600000;

    private final NodeMonitoringRepository repository;
    private final Map<String, Monitoring> monitoringHolder = new ConcurrentHashMap<>();

    @Override
    public Single<Monitoring> createOrUpdate(Monitoring monitoring) {
        if (repository == null) {
            log.debug("There is nowhere to persist the monitoring data {}", monitoring);
            return Single.just(monitoring);
        }

        final String monitoringKey = monitoring.getNodeId() + monitoring.getType();

        Maybe<Monitoring> monitoringObs;

        // This part is mainly done for clustering purpose, to ensure all nodes know about each other in the context
        // of a cluster once they are flagged as master to process node infos.
        if (monitoringHolder.containsKey(monitoringKey)) {
            monitoringObs = Maybe.just(monitoringHolder.get(monitoringKey));
        } else {
            monitoringObs = repository.findByNodeIdAndType(monitoring.getNodeId(), monitoring.getType());
        }

        final Date now = new Date();
        return monitoringObs
            .flatMap(existing -> {
                monitoring.setId(existing.getId());
                monitoring.setCreatedAt(existing.getCreatedAt());
                monitoring.setUpdatedAt(now);
                return repository.update(monitoring).toMaybe();
            })
            .switchIfEmpty(
                Single.defer(() -> {
                    monitoring.setId(UUID.random().toString());
                    monitoring.setCreatedAt(now);
                    monitoring.setUpdatedAt(now);
                    return repository.create(monitoring);
                })
            )
            .doOnSuccess(toCache -> monitoringHolder.put(monitoringKey, toCache))
            .doFinally(this::cleanupMonitorHolder);
    }

    /**
     * Remove all Monitor that have not been updated since {@link #CLEANUP_DELAY} milliseconds.
     */
    private void cleanupMonitorHolder() {
        final Date cleanupDate = new Date(System.currentTimeMillis() - CLEANUP_DELAY);
        monitoringHolder.entrySet().removeIf(entry -> entry.getValue().getUpdatedAt().before(cleanupDate));
    }

    @Override
    public Flowable<Monitoring> findByTypeAndTimeframe(String type, long from, long to) {
        if (repository == null) {
            return Flowable.empty();
        }

        return repository.findByTypeAndTimeFrame(type, from, to);
    }
}
