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
package io.gravitee.node.jetty;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.node.jetty.handler.NoContentOutputErrorHandler;
import io.gravitee.node.jetty.spring.JettyContainerConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Import({ JettyContainerConfiguration.class })
public abstract class JettyHttpServer extends AbstractLifecycleComponent<JettyHttpServer> {

    private final Logger logger = LoggerFactory.getLogger(JettyHttpServer.class);

    @Autowired
    protected JettyHttpServerFactory serverFactory;

    protected Server server;

    /**
     * Allows to attach any handlers to the Jetty Http Server before it starts (ex : jaxrs with security configuration).
     */
    protected abstract void attachHandlers();

    @Override
    protected void doStart() throws Exception {
        server = serverFactory.getObject();

        // This part is needed to avoid WARN while starting container.
        this.attachNoContentHandler();

        // Attach all custom handlers.
        this.attachHandlers();

        server.setStopAtShutdown(true);

        try {
            server.join();

            // Start HTTP server...
            server.start();

            logger.info("HTTP Server is now started and listening on port {}", ((ServerConnector) server.getConnectors()[0]).getPort());
        } catch (InterruptedException ex) {
            logger.error("An error occurs while trying to initialize HTTP server", ex);
            throw ex;
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private void attachNoContentHandler() {
        AbstractHandler noContentHandler = new NoContentOutputErrorHandler();

        // This part is needed to avoid WARN while starting container.
        noContentHandler.setServer(server);
        server.addBean(noContentHandler);
    }
}
