package io.gravitee.node.management.http.node.log;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.LoggerContext;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LoggingEndpointTest {

    private LoggingEndpoint cut;

    @Mock
    private RoutingContext routingContext;

    @Mock
    private HttpServerResponse httpServerResponse;

    @BeforeEach
    void init() {
        when(routingContext.response()).thenReturn(httpServerResponse);

        cut = new LoggingEndpoint();
    }

    @Test
    void should_return_logger_level() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.method()).thenReturn(HttpMethod.GET);
        when(routingContext.request()).thenReturn(request);

        Map<String, Object> loggersMap = getCurrentLogger();
        String expectedOutput = new JsonObject(loggersMap).encode();

        cut.handle(routingContext);

        verify(httpServerResponse).setStatusCode(HttpStatusCode.OK_200);
        verify(httpServerResponse).end(expectedOutput);
    }

    @Test
    void should_change_logger_level() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.method()).thenReturn(HttpMethod.POST);
        when(routingContext.request()).thenReturn(request);

        var requestBody = mock(RequestBody.class);
        Map<String, Object> body = Map.of("io.gravitee.node.management.http.node.log.LoggingEndpointTest", "DEBUG");
        when(requestBody.asJsonObject()).thenReturn(new JsonObject(body));

        Map<String, Object> loggersMap = getCurrentLogger();
        loggersMap.put("io.gravitee.node.management.http.node.log.LoggingEndpointTest", "DEBUG");
        String expectedOutput = new JsonObject(loggersMap).encode();

        when(routingContext.body()).thenReturn(requestBody);
        cut.handle(routingContext);

        verify(httpServerResponse).setStatusCode(HttpStatusCode.OK_200);
        verify(httpServerResponse).end(expectedOutput);
    }

    @Test
    void should_reset_logger_level_when_level_empty() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.method()).thenReturn(HttpMethod.POST);
        when(routingContext.request()).thenReturn(request);

        var requestBody = mock(RequestBody.class);
        Map<String, Object> body = Map.of("io.gravitee.node.management.http.node.log.AnotherClass", "DEBUG");
        when(requestBody.asJsonObject()).thenReturn(new JsonObject(body));

        Map<String, Object> loggersMap = getCurrentLogger();
        String secondOutput = new JsonObject(loggersMap).encode();
        loggersMap.put("io.gravitee.node.management.http.node.log.AnotherClass", "DEBUG");
        String firstOutput = new JsonObject(loggersMap).encode();

        when(routingContext.body()).thenReturn(requestBody);
        cut.handle(routingContext);

        //Send the body again with an empty value for the same logger
        body = Map.of("io.gravitee.node.management.http.node.log.AnotherClass", "");
        when(requestBody.asJsonObject()).thenReturn(new JsonObject(body));

        cut.handle(routingContext);

        InOrder inOrder = inOrder(httpServerResponse);
        inOrder.verify(httpServerResponse).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        inOrder.verify(httpServerResponse).setStatusCode(HttpStatusCode.OK_200);
        inOrder.verify(httpServerResponse).end(firstOutput);
        inOrder.verify(httpServerResponse).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        inOrder.verify(httpServerResponse).setStatusCode(HttpStatusCode.OK_200);
        inOrder.verify(httpServerResponse).end(secondOutput);
    }

    private Map<String, Object> getCurrentLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, Object> loggerLevels = new HashMap<>();
        loggerContext
            .getLoggerList()
            .forEach(l -> {
                if (l.getLevel() != null) {
                    loggerLevels.put(l.getName(), l.getLevel().toString());
                }
            });
        return loggerLevels;
    }
}
