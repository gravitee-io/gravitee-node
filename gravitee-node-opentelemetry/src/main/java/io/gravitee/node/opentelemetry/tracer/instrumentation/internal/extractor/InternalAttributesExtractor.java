/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.node.opentelemetry.tracer.instrumentation.internal.extractor;

import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InternalAttributesExtractor implements AttributesExtractor<InternalRequest, Void> {

    @Override
    public void onStart(
        final AttributesBuilder attributes,
        final io.opentelemetry.context.Context parentContext,
        final InternalRequest internalRequest
    ) {
        if (internalRequest.attributes() != null) {
            internalRequest.attributes().forEach(attributes::put);
        }
    }

    @Override
    public void onEnd(
        final AttributesBuilder attributes,
        final io.opentelemetry.context.Context context,
        final InternalRequest httpRequest,
        final Void response,
        final Throwable error
    ) {}
}
