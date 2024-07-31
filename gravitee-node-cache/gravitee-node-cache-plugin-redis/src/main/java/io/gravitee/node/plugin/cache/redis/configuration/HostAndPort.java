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

import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public class HostAndPort {

    private final String host;
    private final int port;
    private String password;
    private boolean useSsl;

    private HostAndPort(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static HostAndPort of(String host, int port) {
        return new HostAndPort(host, port);
    }

    public HostAndPort withPassword(String password) {
        this.password = password;

        return this;
    }

    public HostAndPort withSsl(boolean useSsl) {
        this.useSsl = useSsl;

        return this;
    }

    public String toConnectionString() {
        String connectionType = "redis";

        if (useSsl) {
            connectionType = "rediss";
        }

        if (StringUtils.hasText(password)) {
            return connectionType + "://:" + password + '@' + host + ':' + port;
        }

        return connectionType + "://" + host + ':' + port;
    }
}
