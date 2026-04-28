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
package io.gravitee.node.opentelemetry.exporter.redact;

import io.gravitee.node.api.opentelemetry.redaction.RedactionConfig;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RedactSpanExporter implements SpanExporter {

    private final SpanExporter delegate;
    private final SpanAttributeRedactor redactor;

    public RedactSpanExporter(SpanExporter delegate, RedactionConfig config) {
        this.delegate = delegate;
        this.redactor = new SpanAttributeRedactor(config);
    }

    /** Redacts resource attributes at tracer-creation time. Returns the original if no rule matches. */
    public Resource redactResource(Resource resource) {
        Attributes redacted = redactor.redact(resource.getAttributes());
        if (redacted == resource.getAttributes()) {
            return resource;
        }
        return Resource.create(redacted, resource.getSchemaUrl());
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> spanList = (spans instanceof List<SpanData> l) ? l : new ArrayList<>(spans);
        List<SpanData> result = null;

        for (int i = 0, n = spanList.size(); i < n; i++) {
            SpanData span = spanList.get(i);
            Attributes spanAttrs = span.getAttributes();
            Attributes redactedAttrs = redactor.redact(spanAttrs);
            List<EventData> redactedEvents = redactor.redactEvents(span.getEvents());
            boolean changed = redactedAttrs != spanAttrs || redactedEvents != span.getEvents();

            if (changed && result == null) {
                result = new ArrayList<>(n);
                for (int j = 0; j < i; j++) {
                    result.add(spanList.get(j));
                }
            }
            if (result != null) {
                result.add(changed ? new RedactedSpanData(span, redactedAttrs, redactedEvents) : span);
            }
        }
        return delegate.export(result != null ? result : spans);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
