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
package io.gravitee.node.vertx.verticle.factory;

import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.spi.VerticleFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.Callable;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SpringVerticleFactory implements VerticleFactory, ApplicationContextAware {

    public static final String VERTICLE_PREFIX = "spring";

    private ApplicationContext applicationContext;

    @Override
    public String prefix() {
        return VERTICLE_PREFIX;
    }

    @Override
    public void createVerticle(String verticleName, ClassLoader classLoader, Promise<Callable<Verticle>> promise) {
        String verticleClassname = verticleName.substring(VERTICLE_PREFIX.length() + 1);

        try {
            Class<?> verticleClass = Thread.currentThread().getContextClassLoader().loadClass(verticleClassname);
            promise.complete(() -> (Verticle) applicationContext.getBean(verticleClass));
        } catch (ClassNotFoundException cnfe) {
            promise.fail(cnfe);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
