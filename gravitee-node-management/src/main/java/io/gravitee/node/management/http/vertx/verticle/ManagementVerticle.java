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
package io.gravitee.node.management.http.vertx.verticle;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.management.http.configuration.ConfigurationEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.management.http.metrics.prometheus.PrometheusEndpoint;
import io.gravitee.node.management.http.node.NodeEndpoint;
import io.gravitee.node.management.http.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ManagementVerticle.class
  );

  private static final String PATH = "/_node";
  private static final String WEBHOOK_PATH = "/hooks";

  private static final String AUTHENTICATION_TYPE_NONE = "none";
  private static final String AUTHENTICATION_TYPE_BASIC = "basic";
  private static final String AUTHENTICATION_BASIC_REALM = "gravitee.io";

  @Autowired
  @Qualifier("managementHttpServer")
  private HttpServer httpServer;

  @Autowired
  @Qualifier("managementRouter")
  private Router nodeRouter;

  @Autowired
  @Qualifier("managementWebhookRouter")
  private Router nodeWebhookRouter;

  @Autowired
  private Vertx vertx;

  @Autowired
  @Qualifier("managementAuthProvider")
  private AuthProvider authProvider;

  @Autowired
  private HttpServerConfiguration httpServerConfiguration;

  @Autowired
  private NodeEndpoint nodeEndpoint;

  @Autowired
  private ConfigurationEndpoint configurationEndpoint;

  @Autowired
  private PrometheusEndpoint prometheusEndpoint;

  @Autowired
  private ManagementEndpointManager managementEndpointManager;

  @Autowired
  private Environment environment;

  @Override
  public void start(Promise<Void> promise) throws Exception {
    if (httpServerConfiguration.isEnabled()) {
      doStart(promise);
    } else {
      promise.complete();
      LOGGER.info("Node Management API is disabled, skipping...");
    }
  }

  @Override
  public void stop(Promise<Void> promise) throws Exception {
    if (httpServerConfiguration.isEnabled()) {
      LOGGER.info("Stopping Management API...");
      httpServer.close(
        new Handler<AsyncResult<Void>>() {
          @Override
          public void handle(AsyncResult<Void> event) {
            if (event.succeeded()) {
              LOGGER.info("HTTP Server has been correctly stopped");
              promise.complete();
            } else {
              LOGGER.error(
                "Unexpected error while stopping HTTP listener for Node Management API",
                event.cause()
              );
              promise.fail(event.cause());
            }
          }
        }
      );
    }
  }

  private void doStart(Promise<Void> promise) throws Exception {
    LOGGER.info("Start HTTP listener for Node Management API");

    // Start HTTP server
    Router mainRouter = Router.router(vertx);
    mainRouter.mountSubRouter(WEBHOOK_PATH, nodeWebhookRouter);
    mainRouter.mountSubRouter(PATH, nodeRouter);

    AuthenticationHandler authHandler = null;
    switch (httpServerConfiguration.getAuthenticationType().toLowerCase()) {
      case AUTHENTICATION_TYPE_NONE:
        break;
      case AUTHENTICATION_TYPE_BASIC:
        authHandler =
          BasicAuthHandler.create(authProvider, AUTHENTICATION_BASIC_REALM);
        break;
      default:
        throw new IllegalArgumentException(
          "Unsupported Authentication type " +
          httpServerConfiguration.getAuthenticationType() +
          " for HTTP core services"
        );
    }

    // Set security handler is defined
    if (authHandler != null) {
      mainRouter.route().order(-1).handler(authHandler);
      nodeRouter.route().order(-1).handler(authHandler);
    }

    // Set default handler
    mainRouter.route().handler(ctx -> ctx.fail(HttpStatusCode.NOT_FOUND_404));

    // Add request handler
    httpServer
      .requestHandler(mainRouter)
      .listen(
        event -> {
          if (event.failed()) {
            LOGGER.error(
              "HTTP listener for Node Management can not be started properly",
              event.cause()
            );
            promise.fail(event.cause());
          } else {
            managementEndpointManager.register(nodeEndpoint);
            managementEndpointManager.register(configurationEndpoint);

            // Metrics
            boolean metricsEnabled = environment.getProperty(
              "services.metrics.enabled",
              Boolean.class,
              false
            );
            if (metricsEnabled) {
              // Register Prometheus endpoint
              boolean prometheusEnabled = environment.getProperty(
                "services.metrics.prometheus.enabled",
                Boolean.class,
                true
              );
              if (prometheusEnabled) {
                managementEndpointManager.register(prometheusEndpoint);
              }
            }
            LOGGER.info(
              "HTTP listener for Node Management bind to port TCP:{}",
              event.result().actualPort()
            );
            promise.complete();
          }
        }
      );
  }
}
