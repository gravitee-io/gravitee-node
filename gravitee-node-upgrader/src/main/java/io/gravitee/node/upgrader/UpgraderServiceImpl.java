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
import io.gravitee.node.api.Node;
import io.gravitee.node.api.UpgraderRepository;
import io.gravitee.node.api.service.UpgraderService;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderData;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UpgraderServiceImpl
  extends AbstractService<UpgraderServiceImpl>
  implements UpgraderService<UpgraderServiceImpl> {

  @Autowired
  private UpgraderRepository upgraderRepository;

  @Value("${upgrade.mode:false}")
  private boolean upgradeMode;

  private final Logger logger = LoggerFactory.getLogger(
    UpgraderServiceImpl.class
  );

  @Override
  protected String name() {
    return "Upgrader service";
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    Map<String, Upgrader> upgraderBeans = applicationContext.getBeansOfType(
      Upgrader.class
    );
    upgraderBeans
      .values()
      .stream()
      .sorted(Comparator.comparing(Upgrader::getOrder))
      .forEach(
        upgrader -> {
          String name = upgrader.getClass().getSimpleName();
          try {
            UpgraderData upgraderData = upgraderRepository
              .findById(upgrader.getClass().getName())
              .blockingGet();
            if (upgraderData != null) {
              logger.info("{} is already applied. it will be ignored.", name);
            } else {
              logger.info("Apply {} ...", name);
              upgrader.upgrade();
              upgraderRepository.create(
                new UpgraderData(upgrader.getClass().getName(), new Date())
              );
            }
          } catch (Exception e) {
            e.printStackTrace();
            logger.error("Unable to apply {}. Error: ", name, e);
          }
        }
      );

    if (upgradeMode) {
      Node node = applicationContext.getBean(Node.class);
      node.preStop();
      node.stop();
      node.postStop();
      System.exit(0);
    }
  }
}
