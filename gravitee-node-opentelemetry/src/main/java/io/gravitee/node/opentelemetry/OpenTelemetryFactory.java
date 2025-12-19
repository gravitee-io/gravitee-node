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
package io.gravitee.node.opentelemetry;

import io.gravitee.node.api.opentelemetry.InstrumenterTracerFactory;
import io.gravitee.node.api.opentelemetry.Tracer;
import io.gravitee.node.api.opentelemetry.TracerFactory;
import io.gravitee.node.opentelemetry.configuration.OpenTelemetryConfiguration;
import io.gravitee.node.opentelemetry.exporter.SpanExporterFactory;
import io.gravitee.node.opentelemetry.tracer.OpenTelemetryTracer;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@RequiredArgsConstructor
public class OpenTelemetryFactory implements TracerFactory {

    private static final String DEFAULT_HOST_NAME = "unknown";
    private static final String DEFAULT_IP = "0.0.0.0";
    private static final AttributeKey<String> ATTRIBUTE_KEY_HOSTNAME = AttributeKey.stringKey("hostname");
    private static final AttributeKey<String> ATTRIBUTE_KEY_IP = AttributeKey.stringKey("ip");
    private static final AttributeKey<String> ATTRIBUTE_KEY_SERVICE_NAMESPACE = AttributeKey.stringKey("service.namespace");

    private final OpenTelemetryConfiguration configuration;
    private final SpanExporterFactory spanExporterFactory;

    @Override
    public Tracer createTracer(
        final String serviceInstanceId,
        final String serviceName,
        final String serviceNamespace,
        final String serviceVersion,
        final List<InstrumenterTracerFactory> instrumenterTracerFactories
    ) {
        return createTracer(serviceInstanceId, serviceName, serviceNamespace, serviceVersion, instrumenterTracerFactories, null);
    }

    @Override
    public Tracer createTracer(
        final String serviceInstanceId,
        final String serviceName,
        final String serviceNamespace,
        final String serviceVersion,
        final List<InstrumenterTracerFactory> instrumenterTracerFactories,
        final Map<String, String> additionalResourceAttributes
    ) {
        if (configuration.isTracesEnabled()) {
            final Resource resource = createResource(
                serviceInstanceId,
                serviceName,
                serviceNamespace,
                serviceVersion,
                additionalResourceAttributes
            );

            final OpenTelemetrySdkBuilder builder = OpenTelemetrySdk
                .builder()
                .setPropagators(
                    ContextPropagators.create(
                        TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())
                    )
                );

            SdkTracerProvider tracerProvider = SdkTracerProvider
                .builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporterFactory.getSpanExporter()).build())
                .setResource(resource)
                .build();

            builder.setTracerProvider(tracerProvider);
            OpenTelemetrySdk openTelemetrySdk = builder.build();
            return new OpenTelemetryTracer(openTelemetrySdk, instrumenterTracerFactories);
        } else {
            return new NoOpTracer();
        }
    }

    private Resource createResource(
        final String serviceInstanceId,
        final String serviceName,
        final String serviceNameSpace,
        final String serviceVersion,
        final Map<String, String> additionalResourceAttributes
    ) {
        String hostname;
        String ipv4;

        try {
            hostname = InetAddress.getLocalHost().getHostName();
            ipv4 = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to retrieve current host and ip for OpenTelemetry Tracer, fallback to default value");
            hostname = DEFAULT_HOST_NAME;
            ipv4 = DEFAULT_IP;
        }

        ResourceBuilder resourceBuilder = Resource
            .getDefault()
            .toBuilder()
            .put(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId)
            .put(ResourceAttributes.SERVICE_NAME, serviceName)
            .put(ATTRIBUTE_KEY_SERVICE_NAMESPACE, serviceNameSpace)
            .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
            .put(ATTRIBUTE_KEY_IP, ipv4)
            .put(ATTRIBUTE_KEY_HOSTNAME, hostname)
            .putAll(configuration.getExtraAttributes());

        if (additionalResourceAttributes != null) {
            additionalResourceAttributes.forEach(resourceBuilder::put);
        }

        return resourceBuilder.build();
    }
}
