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
import lombok.Data;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class RedisConfiguration {

    private HostAndPort hostAndPort;
    private SentinelConfiguration sentinelConfiguration;
    private SslOptions sslConfiguration;
    private Integer maxPoolSize;
    private Integer maxPoolWaiting;
    private Integer poolCleanerInterval;
    private Integer poolRecycleTimeout;
    private Integer maxWaitingHandlers;
}
