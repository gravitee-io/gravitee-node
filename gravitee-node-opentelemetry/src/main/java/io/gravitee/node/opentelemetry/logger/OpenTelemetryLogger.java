package io.gravitee.node.opentelemetry.logger;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.Logger;
import io.gravitee.node.opentelemetry.tracer.vertx.VertxContextStorage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.vertx.core.Context;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class OpenTelemetryLogger extends AbstractService<Logger> implements Logger {

    private final OpenTelemetrySdk openTelemetrySdk;
    private io.opentelemetry.api.logs.Logger logger;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        logger = openTelemetrySdk.getSdkLoggerProvider().loggerBuilder("gravitee").build();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        openTelemetrySdk.close();
    }

    @Override
    public void record(Context vertxContext, String body) {
        this.record(vertxContext, body, Map.of());
    }

    @Override
    public void record(Context vertxContext, String body, Map<String, Object> attributes) {
        io.opentelemetry.context.Context openTelemetryContext = VertxContextStorage.getContext(vertxContext);

        AttributesBuilder builder = Attributes.builder();
        attributes.forEach((key, value) -> {
            switch (value) {
                case String s -> builder.put(key, s);
                case Integer i -> builder.put(key, i);
                case Long l -> builder.put(key, l);
                default -> builder.put(key, String.valueOf(value));
            }
        });

        logger.logRecordBuilder().setAllAttributes(builder.build()).setContext(openTelemetryContext).setBody(body).emit();
    }
}
