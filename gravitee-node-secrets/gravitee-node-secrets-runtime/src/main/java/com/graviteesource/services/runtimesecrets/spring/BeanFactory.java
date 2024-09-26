package com.graviteesource.services.runtimesecrets.spring;

import static com.graviteesource.services.runtimesecrets.config.Config.ALLOW_EMPTY_ACL_SPECS;
import static com.graviteesource.services.runtimesecrets.config.Config.ALLOW_ON_THE_FLY_SPECS;

import com.graviteesource.services.runtimesecrets.RuntimeSecretProcessingService;
import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.discovery.ContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.DefinitionBrowserRegistry;
import com.graviteesource.services.runtimesecrets.el.ContextUpdater;
import com.graviteesource.services.runtimesecrets.grant.DefaultGrantService;
import com.graviteesource.services.runtimesecrets.grant.GrantRegistry;
import com.graviteesource.services.runtimesecrets.providers.DefaultRuntimeResolver;
import com.graviteesource.services.runtimesecrets.providers.FromConfigurationSecretProviderDeployer;
import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import com.graviteesource.services.runtimesecrets.spec.DefaultSpecLifecycleService;
import com.graviteesource.services.runtimesecrets.spec.registry.EnvAwareSpecRegistry;
import com.graviteesource.services.runtimesecrets.storage.SimpleOffHeapCache;
import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class BeanFactory {

    @Bean
    Config config(
        @Value("${" + ALLOW_ON_THE_FLY_SPECS + ":true}") boolean allowRuntimeSpecs,
        @Value("${" + ALLOW_EMPTY_ACL_SPECS + ":true}") boolean allowEmptyACLSpecs
    ) {
        return new Config(allowRuntimeSpecs, allowEmptyACLSpecs);
    }

    @Bean
    RuntimeSecretProcessingService runtimeSecretProcessingService(
        DefinitionBrowserRegistry definitionBrowserRegistry,
        SpecLifecycleService specLifecycleService,
        GrantService grantService,
        EnvAwareSpecRegistry specRegistry
    ) {
        return new RuntimeSecretProcessingService(
            definitionBrowserRegistry,
            new ContextRegistry(),
            grantService,
            specLifecycleService,
            specRegistry
        );
    }

    @Bean
    DefinitionBrowserRegistry definitionBrowserRegistry(List<DefinitionBrowser> browsers) {
        return new DefinitionBrowserRegistry(browsers);
    }

    @Bean
    SpecLifecycleService secretSpecService(Cache cache, ResolverService resolverService, Config config) {
        return new DefaultSpecLifecycleService(new EnvAwareSpecRegistry(), cache, resolverService, config);
    }

    @Bean
    Cache secretCache() {
        return new SimpleOffHeapCache();
    }

    @Bean
    GrantService grantService(Config config) {
        return new DefaultGrantService(new GrantRegistry(), config);
    }

    @Bean
    EnvAwareSpecRegistry envAwareSpecRegistry() {
        return new EnvAwareSpecRegistry();
    }

    @Bean
    @Conditional(EnvironmentCondition.class)
    SecretProviderDeployer runtimeSecretProviderDeployer(Environment environment) {
        return new FromConfigurationSecretProviderDeployer(environment);
    }

    @Bean
    ResolverService runtimeSecretResolver() {
        SecretProviderRegistry secretProviderRegistry = new SecretProviderRegistry();
        return new DefaultRuntimeResolver(secretProviderRegistry);
    }

    @Bean
    ContextUpdater elContextUpdater(
        Cache cache,
        GrantService grantService,
        SpecLifecycleService specLifecycleService,
        EnvAwareSpecRegistry specRegistry
    ) {
        return new ContextUpdater(cache, grantService, specLifecycleService, specRegistry);
    }

    static class EnvironmentCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata ignore) {
            return context.getEnvironment().getProperty("api.secrets.allowProvidersFromConfiguration", Boolean.class, true);
        }
    }
}
