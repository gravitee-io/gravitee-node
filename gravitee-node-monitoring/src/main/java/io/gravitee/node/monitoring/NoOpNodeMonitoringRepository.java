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

import io.gravitee.node.api.Monitoring;
import io.gravitee.node.api.NodeMonitoringRepository;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * Implementation of {@link NodeMonitoringRepository} which does nothing an can be useful when we don't want to persist monitoring node data.
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpNodeMonitoringRepository implements NodeMonitoringRepository {

    @Override
    public Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type) {
        return Maybe.empty();
    }

    @Override
    public Single<Monitoring> create(Monitoring monitoring) {
        return Single.just(monitoring);
    }

    @Override
    public Single<Monitoring> update(Monitoring monitoring) {
        return Single.just(monitoring);
    }

    @Override
    public Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to) {
        return Flowable.empty();
    }
}
