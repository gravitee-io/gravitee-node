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
package io.gravitee.node.vertx.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ListenerConfiguration {

    private HttpServerConfiguration[] httpServerConfiguration;

    private ListenerConfiguration(HttpServerConfiguration[] httpServerConfigurations) {
        this.httpServerConfiguration = httpServerConfigurations;
    }

    public static ListenerConfiguration.ListenerConfigurationBuilder builder() {
        return new ListenerConfiguration.ListenerConfigurationBuilder();
    }

    public HttpServerConfiguration[] getHttpServerConfiguration() {
        return httpServerConfiguration;
    }

    public void setHttpServerConfiguration(HttpServerConfiguration[] httpServerConfiguration) {
        this.httpServerConfiguration = httpServerConfiguration;
    }

    // Builder
    public static class ListenerConfigurationBuilder {

        List<HttpServerConfiguration> configurationList = new ArrayList<>();

        public ListenerConfigurationBuilder addHttpServerConfiguration(HttpServerConfiguration configuration) {
            this.configurationList.add(configuration);
            return this;
        }

        public ListenerConfiguration build() {
            return new ListenerConfiguration(configurationList.toArray(new HttpServerConfiguration[0]));
        }
    }
}
