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

import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JettyHttpServerFactory implements FactoryBean<Server> {

    private static final String KEYSTORE_TYPE_PKCS12 = "pkcs12";

    @Autowired
    private JettyHttpConfiguration jettyHttpConfiguration;

    @Override
    public Server getObject() {
        // Setup ThreadPool
        QueuedThreadPool threadPool = new QueuedThreadPool(
            jettyHttpConfiguration.getPoolMaxThreads(),
            jettyHttpConfiguration.getPoolMinThreads(),
            jettyHttpConfiguration.getPoolIdleTimeout(),
            new ArrayBlockingQueue<>(jettyHttpConfiguration.getPoolQueueSize())
        );
        threadPool.setName("gravitee-listener");

        Server server = new Server(threadPool);

        // Extra options
        server.setDumpAfterStart(false);
        server.setDumpBeforeStop(false);
        server.setStopAtShutdown(true);

        // Setup JMX
        if (jettyHttpConfiguration.isJmxEnabled()) {
            MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            server.addBean(mbContainer);
        }

        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(jettyHttpConfiguration.getOutputBufferSize());
        httpConfig.setRequestHeaderSize(jettyHttpConfiguration.getRequestHeaderSize());
        httpConfig.setResponseHeaderSize(jettyHttpConfiguration.getResponseHeaderSize());
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendDateHeader(false);
        httpConfig.setRequestCookieCompliance(CookieCompliance.RFC2965);
        httpConfig.setResponseCookieCompliance(CookieCompliance.RFC2965);

        // Setup Jetty HTTP or HTTPS Connector
        if (jettyHttpConfiguration.isSecured()) {
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(jettyHttpConfiguration.getHttpPort());

            // SSL Context Factory
            SslContextFactory sslContextFactory = new SslContextFactory.Server.Server();

            if (jettyHttpConfiguration.getKeyStorePath() != null) {
                sslContextFactory.setKeyStorePath(jettyHttpConfiguration.getKeyStorePath());
                sslContextFactory.setKeyStorePassword(jettyHttpConfiguration.getKeyStorePassword());

                if (KEYSTORE_TYPE_PKCS12.equalsIgnoreCase(jettyHttpConfiguration.getKeyStoreType())) {
                    sslContextFactory.setKeyStoreType(KEYSTORE_TYPE_PKCS12);
                }
            }

            if (jettyHttpConfiguration.getTrustStorePath() != null) {
                sslContextFactory.setTrustStorePath(jettyHttpConfiguration.getTrustStorePath());
                sslContextFactory.setTrustStorePassword(jettyHttpConfiguration.getTrustStorePassword());

                if (KEYSTORE_TYPE_PKCS12.equalsIgnoreCase(jettyHttpConfiguration.getTrustStoreType())) {
                    sslContextFactory.setTrustStoreType(KEYSTORE_TYPE_PKCS12);
                }
            }

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            ServerConnector https = new ServerConnector(
                server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig)
            );
            https.setHost(jettyHttpConfiguration.getHttpHost());
            https.setPort(jettyHttpConfiguration.getHttpPort());
            server.addConnector(https);
        } else {
            ServerConnector http = new ServerConnector(
                server,
                jettyHttpConfiguration.getAcceptors(),
                jettyHttpConfiguration.getSelectors(),
                new HttpConnectionFactory(httpConfig)
            );
            http.setHost(jettyHttpConfiguration.getHttpHost());
            http.setPort(jettyHttpConfiguration.getHttpPort());
            http.setIdleTimeout(jettyHttpConfiguration.getIdleTimeout());

            server.addConnector(http);
        }

        // Setup Jetty statistics
        if (jettyHttpConfiguration.isStatisticsEnabled()) {
            StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(server.getHandler());
            server.setHandler(stats);
        }

        if (jettyHttpConfiguration.isAccessLogEnabled()) {
            RequestLogWriter requestLogWriter = new AsyncRequestLogWriter(jettyHttpConfiguration.getAccessLogPath());
            requestLogWriter.setRetainDays(90);
            requestLogWriter.setTimeZone("GMT");

            CustomRequestLog requestLog = new CustomRequestLog(requestLogWriter, CustomRequestLog.EXTENDED_NCSA_FORMAT);
            server.setRequestLog(requestLog);
        }

        return server;
    }

    @Override
    public Class<?> getObjectType() {
        return Server.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
