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
package io.gravitee.node.tracing.spring;

import io.gravitee.node.tracing.LazyTracer;
import io.gravitee.node.tracing.TracingService;
import io.gravitee.node.tracing.vertx.LazyVertxTracerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class TracingConfiguration {

  @Bean
  public TracingService tracingService() {
    return new TracingService();
  }

  @Bean
  public LazyVertxTracerFactory vertxTracerFactory(
    TracingService tracingService
  ) {
    return new LazyVertxTracerFactory(tracingService);
  }

  @Bean
  public LazyTracer lazyTracer(TracingService tracingService) {
    LazyTracer tracer = new LazyTracer();
    tracingService.addTracerListener(tracer);
    return tracer;
  }
}
