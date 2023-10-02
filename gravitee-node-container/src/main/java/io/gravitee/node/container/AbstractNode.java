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
package io.gravitee.node.container;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.ListReverser;
import io.gravitee.common.util.Version;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.License;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.cluster.NodeClusterService;
import io.gravitee.node.license.LicenseService;
import io.gravitee.node.management.http.ManagementService;
import io.gravitee.node.monitoring.handler.NodeMonitoringEventHandler;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.gravitee.node.plugins.service.ServiceManager;
import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.plugin.core.internal.PluginEventListener;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class AbstractNode extends AbstractService<Node> implements Node, ApplicationContextAware {

    private String hostname;

    protected AbstractNode() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            log.warn("Could not get hostname / IP", uhe);
        }
    }

    @Override
    protected void doStart() throws Exception {
        log.info("{} is now starting...", this.name());

        long startTime = System.currentTimeMillis();
        List<Class<? extends LifecycleComponent>> components = this.components();

        preStartComponents(components);
        startComponents(components);
        postStartComponents(components);

        long endTime = System.currentTimeMillis();
        String processId = ManagementFactory.getRuntimeMXBean().getName();
        if (processId.contains("@")) {
            processId = processId.split("@")[0];
        }

        log.info(
            "{} id[{}] version[{}] pid[{}] build[{}#{}] jvm[{}/{}/{}] started in {} ms.",
            this.name(),
            this.id(),
            Version.RUNTIME_VERSION.MAJOR_VERSION,
            processId,
            Version.RUNTIME_VERSION.BUILD_NUMBER,
            Version.RUNTIME_VERSION.REVISION,
            ManagementFactory.getRuntimeMXBean().getVmVendor(),
            ManagementFactory.getRuntimeMXBean().getVmName(),
            ManagementFactory.getRuntimeMXBean().getVmVersion(),
            endTime - startTime
        );
    }

    @Override
    public Node preStop() throws Exception {
        super.preStop();

        final Environment environment = this.applicationContext.getBean(Environment.class);
        final Integer shutdownDelay = environment.getProperty("gracefulShutdown.delay", Integer.class, 0);
        final TimeUnit shutdownUnit = TimeUnit.valueOf(environment.getProperty("gracefulShutdown.unit", String.class, "MILLISECONDS"));

        log.info("Applying graceful shutdown delay {} {}", shutdownDelay, shutdownUnit);
        Thread.sleep(Duration.ofMillis(shutdownUnit.toMillis(shutdownDelay)).toMillis());
        log.info("Graceful shutdown delay exhausted");

        return this;
    }

    @Override
    protected void doStop() throws Exception {
        log.info("{} is stopping", this.name());

        final List<Class<? extends LifecycleComponent>> components = this.components();

        preStopComponents(new ListReverser<>(components));
        stopComponents(new ListReverser<>(components));
        postStopComponents(new ListReverser<>(components));

        log.info("{} stopped", this.name());
    }

    @Override
    public String hostname() {
        return hostname;
    }

    @Override
    public abstract String name();

    @Override
    public List<Class<? extends LifecycleComponent>> components() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

        components.add(LicenseService.class);
        components.add(PluginEventListener.class);
        components.add(PluginRegistry.class);
        components.add(NodeClusterService.class);
        components.add(ServiceManager.class);
        components.add(ManagementService.class);
        components.add(NodeMonitoringEventHandler.class);
        components.add(NodeInfosService.class);
        components.add(NodeHealthCheckService.class);
        components.add(NodeMonitorService.class);
        components.add(ReporterManager.class);
        components.add(KeyStoreLoaderManager.class);

        return components;
    }

    @Override
    public License license() {
        return applicationContext.getBean(LicenseService.class).getLicense();
    }

    private void preStartComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                log.debug("\tPre-starting component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).preStart();
            } catch (Exception e) {
                log.error("An error occurred while pre-starting component {}", componentClass, e);
                throw e;
            }
        }
    }

    private void startComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                log.info("\tStarting component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).start();
            } catch (Exception e) {
                log.error("An error occurred while starting component {}", componentClass, e);
                throw e;
            }
        }
    }

    private void postStartComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                log.debug("\tPost-starting component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).postStart();
            } catch (Exception e) {
                log.error("An error occurred while post-starting component {}", componentClass, e);
                throw e;
            }
        }
    }

    private void preStopComponents(Iterable<Class<? extends LifecycleComponent>> components) {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LifecycleComponent<?> lifecycleComponent = this.applicationContext.getBean(componentClass);
                if (lifecycleComponent.lifecycleState() != Lifecycle.State.STOPPING) {
                    log.debug("\tPre-stopping component: {}", componentClass.getSimpleName());
                    lifecycleComponent.preStop();
                }
            } catch (Exception e) {
                log.error("An error occurred while pre-stopping component {}", componentClass.getSimpleName(), e);
            }
        }
    }

    private void stopComponents(Iterable<Class<? extends LifecycleComponent>> components) {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LifecycleComponent<?> lifecycleComponent = this.applicationContext.getBean(componentClass);
                if (lifecycleComponent.lifecycleState() != Lifecycle.State.STOPPED) {
                    log.info("\tStopping component: {}", componentClass.getSimpleName());
                    lifecycleComponent.stop();
                }
            } catch (Exception e) {
                log.error("An error occurred while stopping component {}", componentClass.getSimpleName(), e);
            }
        }
    }

    private void postStopComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                log.debug("\tPost-stopping component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).postStop();
            } catch (Exception e) {
                log.error("An error occurred while post-stopping component {}", componentClass, e);
                throw e;
            }
        }
    }
}
