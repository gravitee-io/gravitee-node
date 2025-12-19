/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.container.spring.env;

import io.gravitee.node.api.resolver.PropertyResolverFactoriesLoader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@CustomLog
public class PropertiesConfiguration {

    public static final String GRAVITEE_CONFIGURATION = "gravitee.conf";

    @Bean(name = "graviteeProperties")
    public Properties graviteeProperties() throws IOException {
        log.info("Loading Gravitee configuration.");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        String yamlConfigurations = System.getProperty(GRAVITEE_CONFIGURATION);
        if (yamlConfigurations == null) {
            throw new IllegalArgumentException(GRAVITEE_CONFIGURATION + " must be set");
        }

        List<FileSystemResource> yamlResources = Stream.of(yamlConfigurations.split(",")).map(FileSystemResource::new).toList();

        boolean first = true;
        for (FileSystemResource yamlResource : yamlResources) {
            final String action;
            if (first) {
                action = "loaded";
                first = false;
            } else {
                action = "overridden";
            }
            log.info("\tGravitee configuration {} from {}", action, yamlResource.getURL().getPath());
        }

        yaml.setResources(yamlResources.toArray(new FileSystemResource[] {}));

        Properties properties = yaml.getObject();
        log.info("Loading Gravitee configuration. DONE");

        return properties;
    }

    @Bean
    public PropertyResolverFactoriesLoader propertyResolverFactoriesLoader() {
        return new PropertyResolverFactoriesLoader();
    }
}
