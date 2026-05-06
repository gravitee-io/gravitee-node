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
package io.gravitee.node.opentelemetry.logger;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.opentelemetry.Logger;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.opentelemetry.tracer.span.OpenTelemetrySpan;
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
    public void record(final Context vertxContext, final String body) {
        this.record(vertxContext, body, Map.of());
    }

    @Override
    public void record(final Context vertxContext, final String body, final Map<String, Object> attributes) {
        this.record(vertxContext, null, body, attributes);
    }

    @Override
    public void record(Context vertxContext, Span span, String body, Map<String, Object> attributes) {
        io.opentelemetry.context.Context openTelemetryContext = VertxContextStorage.getContext(vertxContext);
        if (span instanceof OpenTelemetrySpan<?> openTelemetrySpan) {
            openTelemetryContext = openTelemetrySpan.otelContext();
        }

        AttributesBuilder builder = Attributes.builder();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (value == null) return;
                switch (value) {
                    case String s -> builder.put(key, s);
                    case Integer i -> builder.put(key, (long) i);
                    case Long l -> builder.put(key, l);
                    case Double d -> builder.put(key, d);
                    case Boolean b -> builder.put(key, b);
                    default -> builder.put(key, String.valueOf(value));
                }
            });
        }

        var logRecordBuilder = logger.logRecordBuilder().setAllAttributes(builder.build()).setBody(body);
        if (openTelemetryContext != null) {
            logRecordBuilder.setContext(openTelemetryContext);
        }
        logRecordBuilder.emit();
    }
}
