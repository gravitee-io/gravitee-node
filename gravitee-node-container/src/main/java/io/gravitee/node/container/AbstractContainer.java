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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import java.io.File;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractContainer extends AbstractService<Container> implements Container {

    private static final String GRAVITEE_HOME_PROPERTY = "gravitee.home";
    private static final String GRAVITEE_CONFIGURATION_PROPERTY = "gravitee.conf";

    protected boolean stopped = false;

    public AbstractContainer() {
        initialize();
    }

    protected void initialize() {
        initializeEnvironment();
        initializeLogging();
    }

    protected void initializeEnvironment() {
        // Set system properties if needed
        String graviteeConfiguration = System.getProperty(GRAVITEE_CONFIGURATION_PROPERTY);
        if (graviteeConfiguration == null || graviteeConfiguration.isEmpty()) {
            String graviteeHome = System.getProperty(GRAVITEE_HOME_PROPERTY);
            System.setProperty(GRAVITEE_CONFIGURATION_PROPERTY, graviteeHome + File.separator + "config" + File.separator + "gravitee.yml");
        }
    }

    protected void initializeLogging() {
        String graviteeHome = System.getProperty(GRAVITEE_HOME_PROPERTY);
        String logbackConfiguration = graviteeHome + File.separator + "config" + File.separator + "logback.xml";
        File logbackConfigurationfile = new File(logbackConfiguration);

        // If logback configuration available, load it, else, load default logback configuration
        if (logbackConfigurationfile.exists()) {
            System.setProperty("logback.configurationFile", logbackConfigurationfile.getAbsolutePath());
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            try {
                configurator.doConfigure(logbackConfigurationfile);
            } catch (JoranException e) {
                LoggerFactory.getLogger(this.getClass()).error("An error occurs while initializing logging system", e);
            }

            // Internal status data is printed in case of warnings or errors.
            StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
        }
    }

    @Override
    protected void doStart() throws Exception {
        LoggerFactory.getLogger(AbstractContainer.class).info("Starting {}...", name());

        try {
            final Node node = node();
            node.start();

            // Register shutdown hook
            Thread shutdownHook = new ContainerShutdownHook(node);
            shutdownHook.setName("graviteeio-finalizer");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (Exception ex) {
            LoggerFactory.getLogger(this.getClass()).error("An unexpected error occurs while starting {}", name(), ex);
            stop();
        }
    }

    @Override
    public Container preStop() throws Exception {
        if (!stopped) {
            LoggerFactory.getLogger(this.getClass()).info("Preparing {} for shutting-down...", name());
            node().preStop();
        }
        return this;
    }

    @Override
    protected void doStop() throws Exception {
        if (!stopped) {
            LoggerFactory.getLogger(this.getClass()).info("Shutting-down {}...", name());

            try {
                node().stop();
            } catch (Exception ex) {
                LoggerFactory.getLogger(this.getClass()).error("Unexpected error", ex);
            } finally {
                stopped = true;
            }
        }
    }

    private class ContainerShutdownHook extends Thread {

        private final Node node;

        private ContainerShutdownHook(Node node) {
            this.node = node;
        }

        @Override
        public void run() {
            if (node != null) {
                try {
                    AbstractContainer.this.preStop();
                    AbstractContainer.this.stop();
                } catch (Exception ex) {
                    LoggerFactory.getLogger(this.getClass()).error("Unexpected error while stopping {}", name(), ex);
                }
            }
        }
    }
}
