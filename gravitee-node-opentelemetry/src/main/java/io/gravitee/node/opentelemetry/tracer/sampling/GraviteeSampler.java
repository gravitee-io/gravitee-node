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
package io.gravitee.node.opentelemetry.tracer.sampling;

import io.gravitee.node.api.opentelemetry.sampling.SamplingConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.function.Supplier;

/**
 * Decides once per trace, on its root span, from the {@link SamplingConfig} the supplier returns at that moment.
 * A span with a local parent always follows it, so a trace is recorded whole or not at all.
 */
public class GraviteeSampler implements Sampler {

    private static final AttributeKey<String> HTTP_REQUEST_METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> HTTP_METHOD_LEGACY = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> URL_PATH = AttributeKey.stringKey("url.path");
    private static final AttributeKey<String> HTTP_TARGET_LEGACY = AttributeKey.stringKey("http.target");

    private final Supplier<SamplingConfig> samplingConfig;

    public GraviteeSampler(final Supplier<SamplingConfig> samplingConfig) {
        this.samplingConfig = samplingConfig;
    }

    @Override
    public SamplingResult shouldSample(
        final Context parentContext,
        final String traceId,
        final String name,
        final SpanKind spanKind,
        final Attributes attributes,
        final List<LinkData> parentLinks
    ) {
        final SpanContext parent = Span.fromContext(parentContext).getSpanContext();
        if (parent.isValid() && !parent.isRemote()) {
            return decision(parent.isSampled());
        }

        final SamplingConfig config = currentConfig();
        if (parent.isValid() && config.respectRemoteParent()) {
            return decision(parent.isSampled());
        }
        if (
            !config.matches(firstOf(attributes, HTTP_REQUEST_METHOD, HTTP_METHOD_LEGACY), firstOf(attributes, URL_PATH, HTTP_TARGET_LEGACY))
        ) {
            return SamplingResult.drop();
        }
        return Sampler.traceIdRatioBased(config.ratio()).shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "GraviteeSampler{" + currentConfig() + "}";
    }

    private SamplingConfig currentConfig() {
        final SamplingConfig config = samplingConfig.get();
        return config != null ? config : SamplingConfig.ALWAYS;
    }

    private static SamplingResult decision(final boolean sampled) {
        return sampled ? SamplingResult.recordAndSample() : SamplingResult.drop();
    }

    private static String firstOf(final Attributes attributes, final AttributeKey<String> key, final AttributeKey<String> fallback) {
        final String value = attributes.get(key);
        return value != null ? value : attributes.get(fallback);
    }
}
