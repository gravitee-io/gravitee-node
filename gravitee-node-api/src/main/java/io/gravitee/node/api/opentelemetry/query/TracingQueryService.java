/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.api.opentelemetry.query;

import io.gravitee.node.api.opentelemetry.query.model.Trace;
import io.gravitee.node.api.opentelemetry.query.model.TraceSearchCriteria;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;

/**
 * Backend-agnostic port for querying stored traces. Implementations adapt this contract to a specific tracing backend
 * (Grafana Tempo today; Jaeger / direct OTLP could follow).
 *
 * @author GraviteeSource Team
 */
public interface TracingQueryService {
    /**
     * List traces matching the given filter. The returned {@link Trace} entries carry summary fields only; {@code spans} is
     * always empty — call {@link #getTrace(String)} to fetch a trace's spans.
     *
     * @param criteria filter (tags, time window, limit)
     * @return list of matching traces; emits an empty list when no trace matches
     */
    Single<List<Trace>> searchTraces(TraceSearchCriteria criteria);

    /**
     * Fetch a single trace by id, including its full span tree.
     *
     * @param traceId the trace identifier
     * @return the trace; completes empty when the backend has no trace for that id
     */
    Maybe<Trace> getTrace(String traceId);
}
