package io.gravitee.node.secrets.service.spring;

import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import io.gravitee.node.secrets.service.keystoreloader.SecretProviderKeyStoreLoaderFactory;
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
    ) {
        return new GraviteeConfigurationSecretResolverDispatcher(secretProviderPluginManager, environment);
    }

    @Bean
    public SecretProviderKeyStoreLoaderFactory secretProviderKeyStoreLoaderFactory(
        KeyStoreLoaderManager keyStoreLoaderManager,
        GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher
    ) {
        final SecretProviderKeyStoreLoaderFactory secretProviderKeyStoreLoaderFactory = new SecretProviderKeyStoreLoaderFactory(
            secretResolverDispatcher
        );
        keyStoreLoaderManager.registerFactory(secretProviderKeyStoreLoaderFactory);
        return secretProviderKeyStoreLoaderFactory;
    }
}
