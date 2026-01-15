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
package io.gravitee.node.plugins.service.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.plugins.service.ServiceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceManagerImpl extends AbstractService implements ServiceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManagerImpl.class);

    private final List<AbstractService> services = new ArrayList<>();

    @Override
    public void register(AbstractService service) {
        services.add(service);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        Map<Integer, List<AbstractService>> servicesByOrder = services
            .stream()
            .collect(groupingBy(AbstractService::getOrder, TreeMap::new, toList()));

        for (Map.Entry<Integer, List<AbstractService>> entry : servicesByOrder.entrySet()) {
            List<AbstractService> servicesInGroup = entry.getValue();

            executePhaseInParallel(servicesInGroup, AbstractService::preStart, "pre-starting");

            executePhaseInParallel(
                servicesInGroup,
                service -> {
                    LOGGER.info("Starting service: {}", service.getClass().getSimpleName());
                    service.start();
                },
                "starting"
            );

            executePhaseInParallel(servicesInGroup, AbstractService::postStart, "post-starting");
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        Map<Integer, List<AbstractService>> servicesByOrder = services
            .stream()
            .collect(groupingBy(AbstractService::getOrder, TreeMap::new, toList()));

        List<Integer> reversedKeys = new ArrayList<>(servicesByOrder.keySet());
        Collections.reverse(reversedKeys);

        for (Integer key : reversedKeys) {
            List<AbstractService> servicesInGroup = servicesByOrder.get(key);

            executePhaseInParallel(servicesInGroup, AbstractService::preStop, "pre-stopping");

            executePhaseInParallel(
                servicesInGroup,
                service -> {
                    LOGGER.info("Stopping service: {}", service.getClass().getSimpleName());
                    service.stop();
                },
                "stopping"
            );

            executePhaseInParallel(servicesInGroup, AbstractService::postStop, "post-stopping");
        }
    }

    private void executePhaseInParallel(List<AbstractService> services, ServiceAction action, String phaseName) {
        List<Callable<Void>> tasks = services
            .stream()
            .map(service ->
                (Callable<Void>) () -> {
                    try {
                        action.execute(service);
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected error while {} service {}", phaseName, service.getClass().getSimpleName(), ex);
                    }
                    return null;
                }
            )
            .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.invokeAll(tasks);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted while waiting for {} phase", phaseName, ex);
        }
    }

    @FunctionalInterface
    private interface ServiceAction {
        void execute(AbstractService service) throws Exception;
    }

    @Override
    protected String name() {
        return "Plugins - Services Manager";
    }
}
