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
package com.graviteesource.services.runtimesecrets.spring;

import static com.graviteesource.services.runtimesecrets.config.Config.*;

import com.graviteesource.services.runtimesecrets.Processor;
import com.graviteesource.services.runtimesecrets.RuntimeSecretsService;
import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.config.OnTheFlySpecs;
import com.graviteesource.services.runtimesecrets.config.Renewal;
import com.graviteesource.services.runtimesecrets.config.Retry;
import com.graviteesource.services.runtimesecrets.discovery.DefaultContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.DefinitionBrowserRegistry;
import com.graviteesource.services.runtimesecrets.el.engine.SecretsTemplateVariableProvider;
import com.graviteesource.services.runtimesecrets.grant.DefaultGrantService;
import com.graviteesource.services.runtimesecrets.grant.GrantRegistry;
import com.graviteesource.services.runtimesecrets.providers.DefaultResolverService;
import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import com.graviteesource.services.runtimesecrets.providers.config.FromConfigurationSecretProviderDeployer;
import com.graviteesource.services.runtimesecrets.renewal.RenewalService;
import com.graviteesource.services.runtimesecrets.spec.DefaultSpecLifecycleService;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import com.graviteesource.services.runtimesecrets.storage.SimpleOffHeapCache;
import io.gravitee.node.api.secrets.runtime.discovery.ContextRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class RuntimeSecretsBeanFactory {

    @Bean
    Config config(
        @Value("${" + ON_THE_FLY_SPECS_ENABLED + ":true}") boolean onTheFlySpecsEnabled,
        @Value("${" + ON_THE_FLY_SPECS_ON_ERROR_RETRY_AFTER_DELAY + ":500}") long onTheFlySpecsOnErrorRetryAfterDelay,
        @Value("${" + ON_THE_FLY_SPECS_ON_ERROR_RETRY_AFTER_UNIT + ":MILLISECONDS}") TimeUnit onTheFlySpecsOnErrorRetryAfterUnit,
        @Value("${" + RENEWAL_ENABLED + ":true}") boolean renewalEnabled,
        @Value("${" + RENEWAL_CHECK_DELAY + ":15}") long renewalCheckDelay,
        @Value("${" + RENEWAL_CHECK_UNIT + ":MINUTES}") TimeUnit renewalCheckUnit,
        @Value("${" + RETRY_ON_ERROR_ENABLED + ":false}") boolean retryOnErrorEnabled,
        @Value("${" + RETRY_ON_ERROR_DELAY + ":2}") long retryOnErrorDelay,
        @Value("${" + RETRY_ON_ERROR_UNIT + ":SECONDS}") TimeUnit retryOnErrorUnit,
        @Value("${" + RETRY_ON_ERROR_BACKOFF_FACTOR + ":1.5}") float factor,
        @Value("${" + RETRY_ON_ERROR_BACKOFF_MAX_DELAY + ":60}") int maxDelay
    ) {
        return new Config(
            new OnTheFlySpecs(
                onTheFlySpecsEnabled,
                Duration.of(onTheFlySpecsOnErrorRetryAfterDelay, onTheFlySpecsOnErrorRetryAfterUnit.toChronoUnit())
            ),
            new Retry(retryOnErrorEnabled, retryOnErrorDelay, retryOnErrorUnit, factor, maxDelay),
            new Renewal(renewalEnabled, Duration.of(renewalCheckDelay, renewalCheckUnit.toChronoUnit()))
        );
    }

    @Bean
    RuntimeSecretsService runtimeSecretsService(
        Processor processor,
        SpecLifecycleService specLifecycleService,
        SpecRegistry specRegistry,
        SecretProviderDeployer secretProviderDeployer,
        RenewalService renewalService,
        Environment environment
    ) {
        return new RuntimeSecretsService(
            processor,
            specLifecycleService,
            specRegistry,
            secretProviderDeployer,
            renewalService,
            environment
        );
    }

    @Bean
    Processor processor(
        DefinitionBrowserRegistry definitionBrowserRegistry,
        ContextRegistry contextRegistry,
        SpecRegistry specRegistry,
        SpecLifecycleService specLifecycleService,
        GrantService grantService
    ) {
        return new Processor(definitionBrowserRegistry, contextRegistry, specRegistry, grantService, specLifecycleService);
    }

    @Bean
    ContextRegistry contextRegistry() {
        return new DefaultContextRegistry();
    }

    @Bean
    DefinitionBrowserRegistry definitionBrowserRegistry(List<DefinitionBrowser> browsers) {
        return new DefinitionBrowserRegistry(browsers);
    }

    @Bean
    RenewalService renewalService(ResolverService resolverService, Cache cache, Config config) {
        return new RenewalService(resolverService, cache, config);
    }

    @Bean
    SpecLifecycleService specLifecycleService(
        SpecRegistry specRegistry,
        ContextRegistry contextRegistry,
        Cache cache,
        ResolverService resolverService,
        GrantService grantService,
        Config config,
        RenewalService renewalService
    ) {
        return new DefaultSpecLifecycleService(specRegistry, contextRegistry, cache, resolverService, grantService, renewalService, config);
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
    SpecRegistry envAwareSpecRegistry() {
        return new SpecRegistry();
    }

    @Bean
    SecretProviderRegistry secretProviderRegistry() {
        return new SecretProviderRegistry();
    }

    @Bean
    @Conditional(AllowGraviteeYmlProviders.class)
    SecretProviderDeployer runtimeSecretProviderDeployer(
        ConfigurableEnvironment environment,
        SecretProviderRegistry secretProviderRegistry,
        SecretProviderPluginManager pluginManager
    ) {
        return new FromConfigurationSecretProviderDeployer(environment, secretProviderRegistry, pluginManager);
    }

    @Bean
    @Conditional({ AllowGraviteeYmlProviders.class })
    ResolverService resolverService(SecretProviderRegistry secretProviderRegistry) {
        return new DefaultResolverService(secretProviderRegistry);
    }

    @Bean
    @Conditional({ DenyConfigProviders.class })
    ResolverService runtimeSecretResolver() {
        return null;
    }

    @Bean
    SecretsTemplateVariableProvider secretsTemplateVariableProvider(
        Cache cache,
        GrantService grantService,
        SpecLifecycleService specLifecycleService,
        SpecRegistry specRegistry
    ) {
        return new SecretsTemplateVariableProvider(cache, grantService, specLifecycleService, specRegistry);
    }

    private static final Predicate<ConditionContext> ALLOW_PROVIDERS_FROM_CONFIG = context ->
        context.getEnvironment().getProperty(API_SECRETS_ALLOW_PROVIDERS_FROM_CONFIGURATION, Boolean.class, true);

    static class AllowGraviteeYmlProviders implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata ignore) {
            return ALLOW_PROVIDERS_FROM_CONFIG.test(context);
        }
    }

    static class DenyConfigProviders implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata ignore) {
            return ALLOW_PROVIDERS_FROM_CONFIG.negate().test(context);
        }
    }
}
