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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;

final class RedactedSpanData extends DelegatingSpanData {

    private final Attributes redactedAttributes;
    private final List<EventData> redactedEvents;

    RedactedSpanData(SpanData delegate, Attributes redactedAttributes, List<EventData> redactedEvents) {
        super(delegate);
        this.redactedAttributes = redactedAttributes;
        this.redactedEvents = redactedEvents;
    }

    @Override
    public Attributes getAttributes() {
        return redactedAttributes;
    }

    @Override
    public List<EventData> getEvents() {
        return redactedEvents;
    }
}
