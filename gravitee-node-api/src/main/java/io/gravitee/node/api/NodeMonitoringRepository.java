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
package io.gravitee.node.api;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface NodeMonitoringRepository {

    /**
     * Returns the {@link Monitoring} object corresponding to the specified type for the specified node identifier.
     *
     * @param nodeId the node identifier.
     * @param type the type of monitoring (MONITOR, HEALTH_CHECK, NODE_INFOS).
     *
     * @return the {@link Monitoring} found or none if no corresponding monitoring object has been found.
     */
    Maybe<Monitoring> findByNodeIdAndType(String nodeId, String type);

    /**
     * Creates a {@link Monitoring} object.
     * @param monitoring the monitoring object to create.
     *
     * @return the created {@link Monitoring} object.
     */
    Single<Monitoring> create(Monitoring monitoring);

    /**
     * Updates a {@link Monitoring} object.
     * @param monitoring the monitoring object to update.
     *
     * @return the updated {@link Monitoring} object.
     */
    Single<Monitoring> update(Monitoring monitoring);

    /**
     * Returns all the {@link Monitoring} objects corresponding to the specified type for the specified time frame.
     *
     * @param type the type of monitoring (MONITOR, HEALTH_CHECK, NODE_INFOS).
     * @param from the beginning of the timeframe.
     * @param to the end of the timeframe.
     *
     * @return the {@link Monitoring} found or none if no corresponding monitoring object has been found.
     */
    Flowable<Monitoring> findByTypeAndTimeFrame(String type, long from, long to);
}
