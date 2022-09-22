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
package io.gravitee.node.vertx.spring;

import io.gravitee.node.tracing.spring.TracingConfiguration;
import io.gravitee.node.vertx.VertxFactory;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import(TracingConfiguration.class)
public class VertxConfiguration {

    @Bean
    public VertxFactory vertxFactory() {
        return new VertxFactory();
    }

    // Destroy method is called from the Vertx delegate, no need to call it twice
    // For information, Spring is looking for a "close()" method and call it automatically when destroying a bean
    @Bean(destroyMethod = "")
    public Vertx vertx(io.vertx.core.Vertx vertx) {
        return Vertx.newInstance(vertx);
    }

    @Bean
    public SpringVerticleFactory springVerticleFactory() {
        return new SpringVerticleFactory();
    }
}
