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
package io.gravitee.node.opentelemetry.spring;

import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.node.opentelemetry.exporter.SpanExporterFactory;
import io.gravitee.node.opentelemetry.tracer.instrumentation.internal.InternalInstrumenterTracerFactory;
import io.gravitee.node.opentelemetry.tracer.instrumentation.vertx.VertxHttpInstrumenterTracerFactory;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class OpenTelemetrySpringConfiguration {

    @Bean
    public OpenTelemetryConfiguration openTelemetryConfiguration(final ConfigurableEnvironment environment) {
        return new OpenTelemetryConfiguration(environment);
    }

    @Bean
    public SpanExporterFactory exporterFactory(final OpenTelemetryConfiguration openTelemetryConfiguration, final Vertx vertx) {
        return new SpanExporterFactory(openTelemetryConfiguration, vertx);
    }

    @Bean
    public OpenTelemetryFactory openTelemetryFactory(
        final OpenTelemetryConfiguration openTelemetryConfiguration,
        final SpanExporterFactory spanExporterFactory
    ) {
        return new OpenTelemetryFactory(openTelemetryConfiguration, spanExporterFactory);
    }

    @Bean
    public VertxHttpInstrumenterTracerFactory vertxHttpInstrumenterTracerFactory() {
        return new VertxHttpInstrumenterTracerFactory();
    }

    @Bean
    public InternalInstrumenterTracerFactory internalInstrumenterTracerFactory() {
        return new InternalInstrumenterTracerFactory();
    }
}
