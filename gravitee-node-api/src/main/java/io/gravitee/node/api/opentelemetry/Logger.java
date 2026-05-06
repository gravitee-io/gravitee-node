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
package io.gravitee.node.api.opentelemetry;

import io.gravitee.common.service.Service;
import io.vertx.core.Context;
import java.util.Map;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Logger extends Service<Logger> {
    /**
     * Log a body associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param body the body to be logged
     */
    void record(final Context vertxContext, String body);

    /**
     * Log a body with attributes associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param body the body to be logged
     * @param attributes the attributes to attach to the Log
     */
    void record(final Context vertxContext, String body, Map<String, Object> attributes);

    /**
     * Log a body with attributes associated to this context
     *
     * @param vertxContext current vert context used to store tracing information
     * @param span extract OpenTelemetry context from this span
     * @param body the body to be logged
     * @param attributes the attributes to attach to the Log
     */
    void record(final Context vertxContext, Span span, String body, Map<String, Object> attributes);
}
