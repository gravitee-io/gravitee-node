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
package io.gravitee.node.management.http.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ConfigurationEndpoint implements ManagementEndpoint {

    private static final Set<String> PROPERTY_PREFIXES = new HashSet<>(Arrays.asList("gravitee.", "gravitee_", "GRAVITEE.", "GRAVITEE_"));

    private static final String ENDPOINT_PATH = "/configuration";

    private static final String PROPERTY_SOURCE_CONFIGURATION = "graviteeYamlConfiguration";

    @Autowired
    private AbstractEnvironment environment;

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return ENDPOINT_PATH;
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();
        response.setStatusCode(HttpStatusCode.OK_200);
        response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        response.setChunked(true);

        // Configuration is coming from gravitee.yml
        EnumerablePropertySource nodeConfiguration = (EnumerablePropertySource) environment
            .getPropertySources()
            .get(PROPERTY_SOURCE_CONFIGURATION);

        // Configuration is coming from environment variables
        Map<String, Object> systemEnvironment = environment.getSystemEnvironment();

        Map<String, Object> prefixlessSystemEnvironment = systemEnvironment
            .entrySet()
            .stream()
            .filter(
                new Predicate<Map.Entry<String, Object>>() {
                    @Override
                    public boolean test(Map.Entry<String, Object> entry) {
                        return (entry.getKey().length() > 9 && PROPERTY_PREFIXES.contains(entry.getKey().substring(0, 9)));
                    }
                }
            )
            .collect(Collectors.toMap(entry -> entry.getKey().substring(9), Map.Entry::getValue));

        TreeMap<String, Object> nodeProperties = Arrays
            .stream(nodeConfiguration.getPropertyNames())
            .collect(
                Collectors.toMap(
                    s -> s,
                    (Function<String, String>) s -> environment.getProperty(s),
                    (v1, v2) -> {
                        throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                    },
                    TreeMap::new
                )
            );

        nodeProperties.putAll(prefixlessSystemEnvironment);

        try {
            response
                .write(
                    DatabindCodec
                        .mapper()
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(nodeProperties)
                )
                .onComplete(event -> {
                    if (event.failed()) {
                        response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                        log.error("Unable to transform data object to JSON", event.cause());
                    }

                    response.end();
                });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public class Property implements Comparable {

        private final String key;
        private final Object value;

        public Property(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public int compareTo(Object o) {
            return this.getKey().compareTo(((Property) o).getKey());
        }
    }
}
