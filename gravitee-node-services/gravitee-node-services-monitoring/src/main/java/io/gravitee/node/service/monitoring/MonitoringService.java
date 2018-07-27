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
package io.gravitee.node.service.monitoring;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.service.monitoring.management.MonitorManagementEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MonitoringService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringService.class);

    @Value("${services.monitoring.enabled:true}")
    private boolean enabled;

    @Value("${services.monitoring.delay:5000}")
    private int delay;

    @Value("${services.monitoring.unit:MILLISECONDS}")
    private TimeUnit unit;

    private ExecutorService executorService;

    @Autowired
    private MonitorManagementEndpoint monitorManagementEndpoint;

    @Autowired
    private ManagementEndpointManager managementEndpointManager;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            executorService = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "node-monitor"));

            MonitoringThread monitorThread = new MonitoringThread();
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(monitorThread);

            LOGGER.info("Node monitoring scheduled with fixed delay {} {} ", delay, unit.name());

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    monitorThread, 0, delay, unit);

            managementEndpointManager.register(monitorManagementEndpoint);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            if (! executorService.isShutdown()) {
                LOGGER.info("Stop node monitor");
                executorService.shutdownNow();
            } else {
                LOGGER.info("Gateway monitor already shut-downed");
            }

            super.doStop();
            LOGGER.info("Stop node monitor : DONE");
        }
    }

    @Override
    protected String name() {
        return "Monitoring Service";
    }
}
