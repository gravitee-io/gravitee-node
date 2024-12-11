package io.gravitee.node.secrets.config.spring;

import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.DefaultKeyStoreLoaderFactoryRegistry;
import io.gravitee.node.secrets.config.GraviteeConfigurationSecretResolver;
import io.gravitee.node.secrets.config.keystoreloader.SecretProviderKeyStoreLoaderFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ConfigurationLevelSecretBeanFactory {

    @Bean
    public GraviteeConfigurationSecretResolver nodeGraviteeConfigurationSecretResolver(
        SecretProviderPluginManager secretProviderPluginManager,
        Environment environment
    ) {
        return new GraviteeConfigurationSecretResolver(secretProviderPluginManager, environment);
    }

    @Bean
    public SecretProviderKeyStoreLoaderFactory secretProviderKeyStoreLoaderFactory(
        DefaultKeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        GraviteeConfigurationSecretResolver secretResolverDispatcher
    ) {
        final SecretProviderKeyStoreLoaderFactory secretProviderKeyStoreLoaderFactory = new SecretProviderKeyStoreLoaderFactory(
            secretResolverDispatcher
        );
        keyStoreLoaderFactoryRegistry.registerFactory(secretProviderKeyStoreLoaderFactory);
        return secretProviderKeyStoreLoaderFactory;
    }
}
