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
package io.gravitee.node.api.opentelemetry.query.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter passed to {@link io.gravitee.node.api.opentelemetry.query.TracingQueryService#searchTraces(TraceSearchCriteria)}.
 * <p>
 * {@code tags} is a structured key/value filter — the backend decides where each key lands (resource vs. span attribute) and
 * how it is encoded into the backend query language.
 *
 * @author GraviteeSource Team
 */
public record TraceSearchCriteria(Map<String, String> tags, Integer limit, Instant start, Instant end) {
    public TraceSearchCriteria {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (tags == null) {
            tags = new HashMap<>();
        }
    }
}
