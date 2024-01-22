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
package io.gravitee.node.container.spring;

import io.gravitee.common.component.LifecycleComponent;
import io.gravitee.kubernetes.client.spring.KubernetesClientConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.certificates.spring.NodeCertificatesConfiguration;
import io.gravitee.node.container.AbstractContainer;
import io.gravitee.node.container.spring.env.EnvironmentConfiguration;
import io.gravitee.node.container.spring.env.PropertiesConfiguration;
import io.gravitee.node.license.LicenseLoaderService;
import io.gravitee.node.management.http.spring.ManagementConfiguration;
import io.gravitee.node.monitoring.spring.NodeMonitoringConfiguration;
import io.gravitee.node.secrets.service.spring.SecretServiceConfiguration;
import io.gravitee.node.vertx.spring.VertxConfiguration;
import io.gravitee.plugin.core.internal.BootPluginEventListener;
import io.gravitee.plugin.core.internal.PluginRegistryImpl;
import io.gravitee.plugin.core.spring.BootPluginConfiguration;
import io.gravitee.plugin.core.spring.BootPluginHandlerBeanRegistryPostProcessor;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.plugin.core.spring.PluginHandlerBeanRegistryPostProcessor;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public abstract class SpringBasedContainer extends AbstractContainer {

    private AnnotationConfigApplicationContext ctx;

    @Override
    public void initialize() {
        if (!initialized) {
            initializeEnvironment();
            initializeLogging();
            initializeContext();
        }

        initialized = true;
    }

    protected void initializeContext() {
        log.info("Starting Boot phase.");

        final AnnotationConfigApplicationContext bootCtx = new AnnotationConfigApplicationContext();
        final List<Class<?>> bootstrapClasses = bootstrapClasses();

        // Register all the bootstrap classes into the boot spring context.
        bootstrapClasses.forEach(bootCtx::register);
        bootCtx.refresh();
        startBootstrapComponents(bootCtx);

        log.info("Boot phase done. Initializing context.");

        // Create an application context and inherit from boot context.
        ctx = new AnnotationConfigApplicationContext();
        ctx.setEnvironment(bootCtx.getEnvironment());
        ctx.setParent(bootCtx);

        List<Class<?>> classes = annotatedClasses();
        classes.forEach(ctx::register);

        ctx.refresh();

        log.info("Context initialized.");
    }

    protected void startBootstrapComponents(AnnotationConfigApplicationContext ctx) {
        final List<Class<? extends LifecycleComponent<?>>> componentClasses = bootstrapComponents();
        final PluginRegistryImpl pluginRegistry = ctx.getBean(PluginRegistryImpl.class);
        final List<? extends LifecycleComponent<?>> components = componentClasses.stream().map(ctx::getBean).toList();

        try {
            // Start the components that a required before bootstrapping the plugin registry.
            for (LifecycleComponent<?> component : components) {
                component.preStart();
            }

            for (LifecycleComponent<?> component : components) {
                component.start();
            }

            // Bootstrap the plugin registry to load all boot plugins (e.g.: secret providers, ...).
            pluginRegistry.bootstrap();
        } catch (Exception e) {
            throw new RuntimeException("Unable to bootstrap the components.", e);
        }
    }

    protected void stopBootstrapComponents(AnnotationConfigApplicationContext ctx) {
        final List<Class<? extends LifecycleComponent<?>>> componentClasses = bootstrapComponents();
        final List<? extends LifecycleComponent<?>> components = componentClasses.stream().map(ctx::getBean).toList();

        try {
            for (LifecycleComponent<?> component : components) {
                component.preStop();
            }

            for (LifecycleComponent<?> component : components) {
                component.stop();
            }
        } catch (Exception e) {
            log.error("Unable to shutdown the components.", e);
        }
    }

    protected List<Class<?>> bootstrapClasses() {
        // These classes represent the minimal list of Spring beans configuration to initialize during the boot phase.
        final List<Class<?>> bootstrapClasses = new ArrayList<>();
        bootstrapClasses.add(EnvironmentConfiguration.class);
        bootstrapClasses.add(PropertiesConfiguration.class);
        bootstrapClasses.add(NodeContainerConfiguration.class);
        bootstrapClasses.add(BootPluginConfiguration.class);
        bootstrapClasses.add(SecretServiceConfiguration.class);
        bootstrapClasses.add(NodeCertificatesConfiguration.class);
        bootstrapClasses.add(KubernetesClientConfiguration.class);

        // Bean registry post processor needs to be manually registered as it MUST be taken in account before spring context is refreshed.
        bootstrapClasses.add(BootPluginHandlerBeanRegistryPostProcessor.class);

        return bootstrapClasses;
    }

    protected List<Class<? extends LifecycleComponent<?>>> bootstrapComponents() {
        // These classes represent the list of Gravitee component to start during the boot phase (in order).
        final List<Class<? extends LifecycleComponent<?>>> bootstrapComponentClasses = new ArrayList<>();
        bootstrapComponentClasses.add(LicenseManager.class);
        bootstrapComponentClasses.add(LicenseLoaderService.class);
        bootstrapComponentClasses.add(BootPluginEventListener.class);

        return bootstrapComponentClasses;
    }

    protected List<Class<?>> annotatedClasses() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(VertxConfiguration.class);
        classes.add(PluginConfiguration.class);
        classes.add(ManagementConfiguration.class);
        classes.add(NodeMonitoringConfiguration.class);

        // Bean registry post processor needs to be manually registered as it MUST be taken in account before spring context is refreshed.
        classes.add(PluginHandlerBeanRegistryPostProcessor.class);
        return classes;
    }

    @Override
    protected void doStop() throws Exception {
        if (!stopped) {
            if (log.isInfoEnabled()) {
                log.info("Shutting-down {}...", name());
            }
            try {
                stopBootstrapComponents(ctx);
                node().stop();
            } catch (Exception ex) {
                LoggerFactory.getLogger(this.getClass()).error("Unexpected error", ex);
            } finally {
                ctx.close();
                stopped = true;
            }
        }
    }

    @Override
    public Node node() {
        // Get a reference to the node
        return ctx.getBean(Node.class);
    }

    public ApplicationContext applicationContext() {
        return ctx;
    }
}
