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
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.upgrader.*;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class UpgraderServiceImpl extends AbstractService<UpgraderServiceImpl> implements LifecycleComponent<UpgraderServiceImpl> {

    private final Configuration configuration;
    private final UpgraderRepository upgraderRepository;

    public UpgraderServiceImpl(Configuration configuration, UpgraderRepository upgraderRepository) {
        this.configuration = configuration;
        this.upgraderRepository = upgraderRepository;
    }

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
                    UpgradeRecord upgradeRecord = upgraderRepository.findById(upgrader.identifier()).blockingGet();
                    if (upgradeRecord != null) {
                        log.info("{} is already applied. It will be ignored.", name);
                        return;
                    }

                    log.info("Apply {} ...", name);

                    UpgradeStatus status = runUpgrade(upgrader);
                    log.debug("{} upgrade result: {}", name, status);

                    switch (status) {
                        case SUCCESS -> upgraderRepository.create(new UpgradeRecord(upgrader.identifier(), new Date())).blockingGet();
                        case FAILED_WITH_EXCEPTION -> {
                            log.error("{} update not applied: it {}. ", name, status);
                            stopUpgrade.set(true);
                        }
                        case FAILED -> {
                            log.error(
                                "{} update not applied: it {}. Updater returned false — possibly due to business logic or hidden exception in legacy code.",
                                name,
                                status
                            );
                            stopUpgrade.set(true);
                        }
                    }
                } catch (Exception e) {
                    log.error("Unexpected error while processing {} (likely repository-related).", name, e);
                    stopUpgrade.set(true);
                }
            });

        // The UpgraderService is registered to be executed if upgrader.mode property is either null or true
        //    but we only stop the node if upgrade.mode is set explicitly to "true".
        //    This is to keep the backward compatibility. Please look at UpgraderConfiguration for more details
        if (upgradeMode() || stopUpgrade.get()) {
            Node node = applicationContext.getBean(Node.class);
            node.preStop();
            node.stop();
            node.postStop();
            if (stopUpgrade.get()) {
                log.error("Stopping because one of the upgrades could not be performed");
                System.exit(1);
            } else {
                System.exit(0);
            }
        }
    }

    private boolean upgradeMode() {
        return configuration.getProperty("upgrade.mode", Boolean.class, false);
    }

    private UpgradeStatus runUpgrade(Upgrader upgrader) {
        String name = upgrader.getClass().getSimpleName();
        try {
            boolean status = upgrader.upgrade();
            return status ? UpgradeStatus.SUCCESS : UpgradeStatus.FAILED;
        } catch (UpgraderException e) {
            log.error("Upgrade failed for {}. An exception occurred during execution. Upgrade process will be stopped.", name, e);
            return UpgradeStatus.FAILED_WITH_EXCEPTION;
        }
    }
}
