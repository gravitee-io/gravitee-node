package io.gravitee.node.opentelemetry.logger.noop;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.Logger;
import io.vertx.core.Context;
import java.util.Map;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpLogger extends AbstractService<Logger> implements Logger {

    @Override
    public void record(Context vertxContext, String body) {}

    @Override
    public void record(Context vertxContext, String body, Map<String, Object> attributes) {}
}
