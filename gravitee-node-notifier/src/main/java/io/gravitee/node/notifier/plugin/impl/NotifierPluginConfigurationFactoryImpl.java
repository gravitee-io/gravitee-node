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
package io.gravitee.node.notifier.plugin.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.notifier.plugin.NotifierPluginConfigurationFactory;
import io.gravitee.notifier.api.NotifierConfiguration;
import java.io.IOException;
import lombok.CustomLog;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class NotifierPluginConfigurationFactoryImpl implements NotifierPluginConfigurationFactory {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T extends NotifierConfiguration> T create(Class<T> notifierConfigurationClass, String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            log.debug("Unable to create a Notifier configuration from a null or empty configuration data");
            return null;
        }

        try {
            return mapper.readValue(configuration, notifierConfigurationClass);
        } catch (IOException ex) {
            log.error("Unable to instance Notifier configuration for {}", notifierConfigurationClass.getName(), ex);
        }

        return null;
    }
}
