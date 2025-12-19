package io.gravitee.node.management.http.node.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggingEndpoint implements ManagementEndpoint {

    @Override
    public HttpMethod method() {
        return null;
    }

    @Override
    public List<HttpMethod> methods() {
        return List.of(HttpMethod.POST, HttpMethod.GET);
    }

    @Override
    public String path() {
        return "/logging";
    }

    @Override
    public void handle(RoutingContext context) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // When POST is made, get the body and apply the changes requested
        if (context.request().method() == io.vertx.core.http.HttpMethod.POST) {
            //Extract the value from the body
            context
                .body()
                .asJsonObject()
                .getMap()
                .forEach((loggerName, level) -> {
                    if (loggerName != null) {
                        //If level is empty, set it to null to reset logger to its default
                        Level newLevel = null;
                        if (level != null && !level.toString().isEmpty()) {
                            newLevel = Level.toLevel(level.toString(), null);
                        }
                        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(loggerName);
                        if (logger != null) {
                            logger.setLevel(newLevel);
                        }
                    }
                });
        }

        // Return the current configuration
        HttpServerResponse response = context.response();
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        Map<String, Object> loggerLevels = new HashMap<>();
        loggerContext
            .getLoggerList()
            .forEach(l -> {
                if (l.getLevel() != null) {
                    loggerLevels.put(l.getName(), l.getLevel().toString());
                }
            });
        response.setStatusCode(HttpStatusCode.OK_200);
        response.end(new JsonObject(loggerLevels).encode());
    }
}
