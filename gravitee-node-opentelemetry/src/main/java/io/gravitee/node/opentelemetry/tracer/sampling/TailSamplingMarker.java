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
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Supplier;

/**
 * Marks every span with {@link SamplingConfig#ATTR_TAIL} while tail criteria are configured, so a collector routing on
 * span attributes sends the whole trace to tail sampling instead of splitting it.
 */
public class TailSamplingMarker implements SpanProcessor {

    private static final AttributeKey<Boolean> TAIL = AttributeKey.booleanKey(SamplingConfig.ATTR_TAIL);

    private final Supplier<SamplingConfig> samplingConfig;

    public TailSamplingMarker(final Supplier<SamplingConfig> samplingConfig) {
        this.samplingConfig = samplingConfig;
    }

    @Override
    public void onStart(final Context parentContext, final ReadWriteSpan span) {
        final SamplingConfig config = samplingConfig.get();
        if (config != null && config.hasTailCriteria()) {
            span.setAttribute(TAIL, true);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(final ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
