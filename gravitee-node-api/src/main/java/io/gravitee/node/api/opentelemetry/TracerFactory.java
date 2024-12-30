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
package io.gravitee.node.api.opentelemetry;

import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TracerFactory {
    /**
     * Create a new {@link Tracer} based on given parameters. The underlying Span exporter will be shared across tracer.
     * <p>
     * The tracer has its own life cycle and must be started and stopped accordingly.
     *
     */
    Tracer createTracer(
        final String id,
        final String serviceName,
        final String serviceNamespace,
        final String version,
        final List<InstrumenterTracerFactory> instrumenterTracerFactories
    );
}
