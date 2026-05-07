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

import java.util.Map;

/**
 * Per-request context passed to a {@link io.gravitee.node.api.opentelemetry.query.TracingQueryService} call.
 * <p>
 * Carries two scoping knobs, both backend-agnostic:
 * <ul>
 *   <li>{@code tenant} — backend-level tenant identity. Tempo turns it into {@code X-Scope-OrgID}; backends
 *       without multi-tenancy ignore it.</li>
 *   <li>{@code resourceAttributeFilters} — soft scoping nested inside the tenant. Each entry is injected as an
 *       equality clause on an OTel resource attribute. Tempo emits TraceQL
 *       {@code resource.<key> = "<value>"} clauses joined with {@code &&}; callers can use any attribute key
 *       (e.g. {@code gravitee.environment.id}, {@code service.namespace}, {@code cloud.region}). Empty map
 *       means no extra scoping.</li>
 * </ul>
 * The record is left open for future per-call knobs (e.g. correlation id, caller-supplied auth headers) without
 * forcing a signature change.
 *
 * @author GraviteeSource Team
 */
public record TracingQueryContext(String tenant, Map<String, String> resourceAttributeFilters) {
    public TracingQueryContext {
        // Defensive copy + null-safety so callers can pass null without the consumers having to null-check.
        resourceAttributeFilters = resourceAttributeFilters == null ? Map.of() : Map.copyOf(resourceAttributeFilters);
    }

    /** Empty context — used by callers that need neither tenancy nor any resource-attribute scoping. */
    public static final TracingQueryContext EMPTY = new TracingQueryContext(null, Map.of());

    public static TracingQueryContext forTenant(String tenant) {
        return new TracingQueryContext(tenant, Map.of());
    }

    public static TracingQueryContext of(String tenant, Map<String, String> resourceAttributeFilters) {
        return new TracingQueryContext(tenant, resourceAttributeFilters);
    }
}
