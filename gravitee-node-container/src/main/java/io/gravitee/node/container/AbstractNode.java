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
import io.gravitee.node.management.http.ManagementService;
import io.gravitee.node.monitoring.handler.NodeMonitoringEventHandler;
import io.gravitee.node.monitoring.healthcheck.NodeHealthCheckService;
import io.gravitee.node.monitoring.infos.NodeInfosService;
import io.gravitee.node.monitoring.monitor.NodeMonitorService;
import io.gravitee.node.plugins.service.ServiceManager;
import io.gravitee.node.reporter.ReporterManager;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.plugin.core.internal.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractNode extends AbstractService<Node> implements Node, ApplicationContextAware {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected ApplicationContext applicationContext;

    private String hostname;

    public AbstractNode() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            LOGGER.warn("Could not get hostname / IP", uhe);
        }
    }

    protected void doStart() throws Exception {
        this.LOGGER.info("{} is now starting...", this.name());
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

        this.LOGGER.info(
                "{} id[{}] version[{}] pid[{}] build[{}#{}] jvm[{}/{}/{}] started in {} ms.",
                this.name(), this.id(),
                Version.RUNTIME_VERSION.MAJOR_VERSION, processId,
                Version.RUNTIME_VERSION.BUILD_NUMBER, Version.RUNTIME_VERSION.REVISION,
                ManagementFactory.getRuntimeMXBean().getVmVendor(), ManagementFactory.getRuntimeMXBean().getVmName(),
                ManagementFactory.getRuntimeMXBean().getVmVersion(), endTime - startTime);
    }

    @Override
    public Node preStop() throws Exception {
        super.preStop();

        final Environment environment = this.applicationContext.getBean(Environment.class);
        final Integer shutdownDelay = environment.getProperty("gracefulShutdown.delay", Integer.class, 0);
        final TimeUnit shutdownUnit = TimeUnit.valueOf(environment.getProperty("gracefulShutdown.unit", String.class, "MILLISECONDS"));

        LOGGER.info("Applying graceful shutdown delay {} {}", shutdownDelay, shutdownUnit);
        Thread.sleep(Duration.ofMillis(shutdownUnit.toMillis(shutdownDelay)).toMillis());
        LOGGER.info("Graceful shutdown delay exhausted");

        return this;
    }

    protected void doStop() throws Exception {
        this.LOGGER.info("{} is stopping", this.name());

        final List<Class<? extends LifecycleComponent>> components = this.components();

        preStopComponents(new ListReverser(components));
        stopComponents(new ListReverser(components));
        postStopComponents(new ListReverser(components));

        this.LOGGER.info("{} stopped", this.name());
    }

    @Override
    public String hostname() {
        return hostname;
    }

    public abstract String name();

    @Override
    public List<Class<? extends LifecycleComponent>> components() {
        List<Class<? extends LifecycleComponent>> components = new ArrayList<>();

        components.add(PluginEventListener.class);
        components.add(PluginRegistry.class);
        components.add(ServiceManager.class);
        components.add(ManagementService.class);
        components.add(NodeMonitoringEventHandler.class);
        components.add(NodeInfosService.class);
        components.add(NodeHealthCheckService.class);
        components.add(NodeMonitorService.class);
        components.add(ReporterManager.class);

        return components;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void preStartComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LOGGER.debug(
                        "Pre-starting component: {}",
                        componentClass.getSimpleName()
                );
                this.applicationContext.getBean(componentClass).preStart();
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while pre-starting component {}",
                        componentClass,
                        e
                );
                throw e;
            }
        }
    }

    private void startComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LOGGER.info("Starting component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).start();
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while starting component {}",
                        componentClass,
                        e
                );
                throw e;
            }
        }
    }

    private void postStartComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LOGGER.debug("Post-starting component: {}", componentClass.getSimpleName());
                this.applicationContext.getBean(componentClass).postStart();
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while post-starting component {}",
                        componentClass,
                        e
                );
                throw e;
            }
        }
    }

    private void preStopComponents(Iterable<Class<? extends LifecycleComponent>> components) {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LifecycleComponent<?> lifecycleComponent =
                        this.applicationContext.getBean(componentClass);
                if (lifecycleComponent.lifecycleState() != Lifecycle.State.STOPPING) {
                    this.LOGGER.debug(
                            "Pre-stopping component: {}",
                            componentClass.getSimpleName()
                    );
                    lifecycleComponent.preStop();
                }
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while pre-stopping component {}",
                        componentClass.getSimpleName(),
                        e
                );
            }
        }
    }

    private void stopComponents(Iterable<Class<? extends LifecycleComponent>> components) {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LifecycleComponent<?> lifecycleComponent =
                        this.applicationContext.getBean(componentClass);
                if (lifecycleComponent.lifecycleState() != Lifecycle.State.STOPPED) {
                    this.LOGGER.info(
                            "Stopping component: {}",
                            componentClass.getSimpleName()
                    );
                    lifecycleComponent.stop();
                }
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while stopping component {}",
                        componentClass.getSimpleName(),
                        e
                );
            }
        }
    }

    private void postStopComponents(Iterable<Class<? extends LifecycleComponent>> components) throws Exception {
        for (Class<? extends LifecycleComponent> componentClass : components) {
            try {
                LOGGER.debug(
                        "Post-stopping component: {}",
                        componentClass.getSimpleName()
                );
                this.applicationContext.getBean(componentClass).postStop();
            } catch (Exception e) {
                this.LOGGER.error(
                        "An error occurred while post-stopping component {}",
                        componentClass,
                        e
                );
                throw e;
            }
        }
    }
}