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
package io.gravitee.node.opentelemetry.tracer.span;

import io.gravitee.node.api.opentelemetry.Span;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter
@Accessors(fluent = true)
public class OpenTelemetrySpan<R> implements Span {

    private final boolean root;
    private final io.vertx.core.Context vertxContext;
    private final io.opentelemetry.context.Context otelContext;
    private final Scope scope;
    private final R request;

    public OpenTelemetrySpan(
        final io.vertx.core.Context vertxContext,
        final io.opentelemetry.context.Context otelContext,
        final io.opentelemetry.context.Scope scope,
        final boolean root,
        final R request
    ) {
        this.vertxContext = vertxContext;
        this.otelContext = otelContext;
        this.scope = scope;
        this.root = root;
        this.request = request;
    }

    public R request() {
        return request;
    }

    public io.opentelemetry.api.trace.Span span() {
        return io.opentelemetry.api.trace.Span.fromContext(otelContext());
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public <T> Span withAttribute(final String name, final T value) {
        io.opentelemetry.api.trace.Span span = span();
        if (value != null) {
            if (value instanceof String stringValue) {
                span.setAttribute(name, stringValue);
            } else if (value instanceof Long longValue) {
                span.setAttribute(name, longValue);
            } else if (value instanceof Integer integerValue) {
                span.setAttribute(name, integerValue);
            } else if (value instanceof Boolean booleanValue) {
                span.setAttribute(name, booleanValue);
            } else {
                span.setAttribute(name, String.valueOf(value));
            }
        }
        return this;
    }

    @Override
    public Span inError() {
        io.opentelemetry.api.trace.Span span = span();
        span.setStatus(StatusCode.ERROR);
        return this;
    }

    @Override
    public Span addEvent(final String name, final Map<String, Object> attributes) {
        io.opentelemetry.api.trace.Span span = span();
        if (attributes != null && !attributes.isEmpty()) {
            AttributesBuilder attributesBuilder = Attributes.builder();
            attributes.forEach((key, value) -> {
                if (value instanceof String stringValue) {
                    attributesBuilder.put(key, stringValue);
                } else if (value instanceof Long longValue) {
                    attributesBuilder.put(key, longValue);
                } else if (value instanceof Integer integerValue) {
                    attributesBuilder.put(key, (long) integerValue);
                } else if (value instanceof Double doubleValue) {
                    attributesBuilder.put(key, doubleValue);
                } else if (value instanceof Boolean booleanValue) {
                    attributesBuilder.put(key, booleanValue);
                } else if (value != null) {
                    attributesBuilder.put(key, String.valueOf(value));
                }
            });
            span.addEvent(name, attributesBuilder.build());
        } else {
            span.addEvent(name);
        }
        return this;
    }
}
