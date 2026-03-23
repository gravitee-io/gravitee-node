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
}
