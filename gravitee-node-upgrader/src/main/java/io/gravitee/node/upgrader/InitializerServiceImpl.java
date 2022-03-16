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
package io.gravitee.node.upgrader;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.service.InitializerService;
import io.gravitee.node.api.upgrader.Initializer;
import java.util.Comparator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InitializerServiceImpl
  extends AbstractService<InitializerServiceImpl>
  implements InitializerService<InitializerServiceImpl> {

  private static final Logger logger = LoggerFactory.getLogger(
    InitializerServiceImpl.class
  );

  @Override
  protected String name() {
    return "Initializer service";
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    Map<String, Initializer> initializerBeans = applicationContext.getBeansOfType(
      Initializer.class
    );
    initializerBeans
      .values()
      .stream()
      .sorted(Comparator.comparing(Initializer::getOrder))
      .forEach(
        initializer -> {
          try {
            logger.info("Apply {} ...", initializer.getClass().getSimpleName());
            initializer.initialize();
          } catch (Exception e) {
            logger.error(
              "Unable to apply the initializer {}",
              initializer.getClass().getSimpleName()
            );
          }
        }
      );
  }
}
