/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.node.opentelemetry.tracer.instrumentation.internal;

import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.gravitee.node.opentelemetry.tracer.instrumentation.AbstractInstrumenterTracer;
import io.gravitee.node.opentelemetry.tracer.instrumentation.internal.extractor.InternalAttributesExtractor;
import io.gravitee.node.opentelemetry.tracer.instrumentation.internal.extractor.InternalSpanNameExtractor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class InternalInstrumenterTracer extends AbstractInstrumenterTracer<InternalRequest, Void> {

    private final OpenTelemetry openTelemetry;
    private Instrumenter<InternalRequest, Void> instrumenter;

    @Override
    public String instrumentationName() {
        return "io.gravitee.opentelemetry.internal";
    }

    @Override
    public <R> boolean canHandle(final R request) {
        return request instanceof InternalRequest;
    }

    @Override
    protected Instrumenter<InternalRequest, Void> getRootInstrumenter() {
        return getDefaultInstrumenter();
    }

    @Override
    protected Instrumenter<InternalRequest, Void> getDefaultInstrumenter() {
        if (instrumenter == null) {
            instrumenter = createInstrumenter(openTelemetry);
        }
        return instrumenter;
    }

    private Instrumenter<InternalRequest, Void> createInstrumenter(final OpenTelemetry openTelemetry) {
        InstrumenterBuilder<InternalRequest, Void> serverBuilder = Instrumenter.builder(
            openTelemetry,
            instrumentationName(),
            new InternalSpanNameExtractor()
        );

        return serverBuilder.addAttributesExtractor(new InternalAttributesExtractor()).buildInstrumenter();
    }
}
