package io.gravitee.node.secrets.service.spring;

import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class SecretServiceBeanFactory {

    @Bean
    public GraviteeConfigurationSecretResolverDispatcher nodeSecretResolverDispatcher(
        SecretProviderPluginManager secretProviderPluginManager,
        Environment environment
    ) throws Exception {
        return new GraviteeConfigurationSecretResolverDispatcher(secretProviderPluginManager, environment);
    }
}
