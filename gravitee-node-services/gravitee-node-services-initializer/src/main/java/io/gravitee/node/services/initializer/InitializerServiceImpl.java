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
package io.gravitee.node.services.initializer;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.initializer.Initializer;
import java.util.Comparator;
import java.util.Map;
import lombok.CustomLog;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class InitializerServiceImpl extends AbstractService<InitializerServiceImpl> implements LifecycleComponent<InitializerServiceImpl> {

    @Override
    protected String name() {
        return "Initializer service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        Map<String, Initializer> initializerBeans = applicationContext.getBeansOfType(Initializer.class);
        initializerBeans
            .values()
            .stream()
            .sorted(Comparator.comparing(Initializer::getOrder))
            .forEach(initializer -> {
                try {
                    log.info("Apply {} ...", initializer.getClass().getSimpleName());
                    initializer.initialize();
                } catch (Exception e) {
                    log.error("Unable to apply the initializer {}", initializer.getClass().getSimpleName());
                }
            });
    }
}
