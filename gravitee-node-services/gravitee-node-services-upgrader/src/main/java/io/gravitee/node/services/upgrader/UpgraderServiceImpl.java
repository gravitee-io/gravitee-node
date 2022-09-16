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
package io.gravitee.node.services.upgrader;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.upgrader.UpgradeRecord;
import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderRepository;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UpgraderServiceImpl extends AbstractService<UpgraderServiceImpl> implements LifecycleComponent<UpgraderServiceImpl> {

    @Autowired
    @Lazy
    private UpgraderRepository upgraderRepository;

    @Value("${upgrade.mode:false}")
    private boolean upgradeMode;

    private static final Logger logger = LoggerFactory.getLogger(UpgraderServiceImpl.class);

    @Override
    protected String name() {
        return "Upgrader service";
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        Map<String, Upgrader> upgraderBeans = applicationContext.getBeansOfType(Upgrader.class);

        AtomicBoolean stopUpgrade = new AtomicBoolean(false);
        upgraderBeans
            .values()
            .stream()
            .sorted(Comparator.comparing(Upgrader::getOrder))
            .takeWhile(upgrader -> !stopUpgrade.get())
            .forEach(upgrader -> {
                String name = upgrader.getClass().getSimpleName();
                try {
                    UpgradeRecord upgradeRecord = upgraderRepository.findById(upgrader.getClass().getName()).blockingGet();
                    if (upgradeRecord != null) {
                        logger.info("{} is already applied. it will be ignored.", name);
                    } else {
                        logger.info("Apply {} ...", name);
                        if (upgrader.upgrade()) {
                            upgraderRepository.create(new UpgradeRecord(upgrader.getClass().getName(), new Date()));
                        } else {
                            stopUpgrade.set(true);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to apply {}. Error: ", name, e);
                }
            });

        // The UpgraderService is registered to be executed if upgrader.mode property is either null or true
        //    but we only stop the node if upgrade.mode is set explicitly to "true".
        //    This is to keep the backward compatibility. Please look at UpgraderConfiguration for more details
        if (upgradeMode) {
            Node node = applicationContext.getBean(Node.class);
            node.preStop();
            node.stop();
            node.postStop();
            System.exit(0);
        }
    }
}
