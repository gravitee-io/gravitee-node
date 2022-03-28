/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.tracing.vertx;

import io.gravitee.node.tracing.TracingService;
import io.vertx.core.impl.VertxBuilder;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LazyVertxTracerFactory implements VertxTracerFactory {

    private final TracingService tracingService;

    public LazyVertxTracerFactory(final TracingService tracingService) {
        this.tracingService = tracingService;
    }

    @Override
    public void init(VertxBuilder builder) {
        VertxTracerFactory.super.init(builder);
    }

    @Override
    public VertxTracer tracer(TracingOptions options) {
        LazyVertxTracer tracer = new LazyVertxTracer();
        tracingService.addTracerListener(tracer);
        return tracer;
    }

    @Override
    public TracingOptions newOptions() {
        return VertxTracerFactory.super.newOptions();
    }

    @Override
    public TracingOptions newOptions(JsonObject jsonObject) {
        return VertxTracerFactory.super.newOptions(jsonObject);
    }
}
