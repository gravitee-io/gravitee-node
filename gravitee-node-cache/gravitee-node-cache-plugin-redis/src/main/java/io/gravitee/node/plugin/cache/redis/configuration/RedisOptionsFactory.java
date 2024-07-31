/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.plugin.cache.redis.configuration;

import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.tcp.VertxTcpClientFactory;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class RedisOptionsFactory {

    public static RedisOptions build(RedisConfiguration configuration) {
        return buildRedisOptions(configuration);
    }

    private static RedisOptions buildRedisOptions(RedisConfiguration configuration) {
        final RedisOptions options = new RedisOptions();

        final var sentinelConfiguration = configuration.getSentinelConfiguration();
        if (sentinelConfiguration != null && sentinelConfiguration.isEnabled()) {
            // Sentinels + Redis master / replicas
            log.debug("Redis repository configured to use Sentinel connection");

            options.setType(RedisClientType.SENTINEL);
            List<HostAndPort> sentinelNodes = sentinelConfiguration.getNodes();
            sentinelNodes.forEach(hostAndPort -> options.addConnectionString(hostAndPort.toConnectionString()));

            if (!StringUtils.hasText(sentinelConfiguration.getMaster())) {
                throw new IllegalStateException("Incorrect Sentinel configuration : parameter 'master' is mandatory!");
            }
            options.setMasterName(sentinelConfiguration.getMaster()).setRole(RedisRole.MASTER);
            options.setPassword(sentinelConfiguration.getPassword());
        } else {
            // Standalone Redis
            log.debug("Redis repository configured to use standalone connection");

            options.setType(RedisClientType.STANDALONE);
            options.setConnectionString(configuration.getHostAndPort().toConnectionString());
        }

        if (configuration.getHostAndPort().isUseSsl()) {
            log.debug("Redis repository configured with ssl enabled");
            SslOptions sslConfiguration = configuration.getSslConfiguration();
            options.getNetClientOptions().setSsl(true);
            VertxTcpClientFactory.configureSslClientOption(options.getNetClientOptions(), sslConfiguration);
        }

        // Set max waiting handlers high enough to manage high throughput since we are not using the pooled mode
        options.setMaxWaitingHandlers(1024);
        return options;
    }
}
