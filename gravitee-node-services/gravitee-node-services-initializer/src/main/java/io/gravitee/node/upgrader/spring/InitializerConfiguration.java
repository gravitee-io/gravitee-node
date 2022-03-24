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
package io.gravitee.node.upgrader.spring;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.node.api.service.InitializerService;
import io.gravitee.node.upgrader.InitializerServiceImpl;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class InitializerConfiguration {

  @Bean
  public InitializerService initializerService() {
    return new InitializerServiceImpl();
  }

  public static List<Class<? extends LifecycleComponent>> getComponents() {
    List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

    String upgradeMode = System.getProperty("upgrade.mode");

    if (upgradeMode == null || "false".equals(upgradeMode)) {
      components.add(InitializerService.class);
    }

    return components;
  }
}
