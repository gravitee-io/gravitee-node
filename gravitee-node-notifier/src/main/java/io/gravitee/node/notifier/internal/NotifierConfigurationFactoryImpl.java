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
package io.gravitee.node.notifier.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.notifier.NotifierConfigurationFactory;
import io.gravitee.notifier.api.NotificationConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class NotifierConfigurationFactoryImpl implements NotifierConfigurationFactory {

    private final Logger LOGGER = LoggerFactory.getLogger(NotifierConfigurationFactoryImpl.class);

    @Autowired
    private ObjectMapper mapper;

    @Override
    public <T extends NotificationConfiguration> T create(Class<T> notifierConfigurationClass, String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            LOGGER.error("Unable to create a notifier configuration from a null or empty configuration data");
            return null;
        }

        try {
            return mapper.readValue(configuration, notifierConfigurationClass);
        } catch (IOException ex) {
            LOGGER.error("Unable to instance notifier configuration for {}", notifierConfigurationClass.getName(), ex);
        }

        return null;
    }
}
