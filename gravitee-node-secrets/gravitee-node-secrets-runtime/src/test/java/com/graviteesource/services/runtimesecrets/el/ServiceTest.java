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
package com.graviteesource.services.runtimesecrets.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.config.OnTheFlySpecs;
import com.graviteesource.services.runtimesecrets.config.Renewal;
import com.graviteesource.services.runtimesecrets.discovery.DefaultContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.RefParser;
import com.graviteesource.services.runtimesecrets.el.engine.SecretSpelTemplateEngine;
import com.graviteesource.services.runtimesecrets.el.engine.SecretsTemplateVariableProvider;
import com.graviteesource.services.runtimesecrets.grant.DefaultGrantService;
import com.graviteesource.services.runtimesecrets.grant.GrantRegistry;
import com.graviteesource.services.runtimesecrets.providers.DefaultResolverService;
import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import com.graviteesource.services.runtimesecrets.renewal.RenewalService;
import com.graviteesource.services.runtimesecrets.spec.DefaultSpecLifecycleService;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import com.graviteesource.services.runtimesecrets.storage.SimpleOffHeapCache;
import io.gravitee.el.spel.SpelExpressionParser;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryLocation;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.secrets.plugin.mock.MockSecretProvider;
import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.security.util.InMemoryResource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ServiceTest {

    public static final String ENV_ID = "foo";
    InMemoryResource inMemoryResource = new InMemoryResource(
        """
                    secrets:
                      mySecret:
                          redisPassword: "redisadmin"
                          ldapPassword: "ldapadmin"
                                                                                                
                      """
    );
    private GrantService grantService;
    private SpecLifecycleService specLifeCycleService;
    private Cache cache;
    private SpelTemplateEngine spelTemplateEngine;

    @BeforeEach
    void before() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(inMemoryResource);
        SecretProviderRegistry secretProviderRegistry = new SecretProviderRegistry();
        secretProviderRegistry.register(
            "mock",
            new MockSecretProvider(new MockSecretProviderConfiguration((Map) new LinkedHashMap<>(yaml.getObject()))),
            null
        );
        cache = new SimpleOffHeapCache();
        Config config = new Config(false, new OnTheFlySpecs(true, Duration.ZERO), new Renewal(true, Duration.ZERO));
        this.grantService = new DefaultGrantService(new GrantRegistry(), config);
        SpecRegistry specRegistry = new SpecRegistry();
        ResolverService resolverService = new DefaultResolverService(secretProviderRegistry);
        RenewalService renewalService = new RenewalService(resolverService, cache, config);
        specLifeCycleService =
            new DefaultSpecLifecycleService(
                specRegistry,
                new DefaultContextRegistry(),
                cache,
                resolverService,
                grantService,
                renewalService,
                config
            );
        SecretsTemplateVariableProvider secretsTemplateVariableProvider = new SecretsTemplateVariableProvider(
            cache,
            grantService,
            specLifeCycleService,
            specRegistry
        );
        spelTemplateEngine = new SecretSpelTemplateEngine(new SpelExpressionParser());
        // set up EL variables
        secretsTemplateVariableProvider.provide(spelTemplateEngine.getTemplateContext());
        spelTemplateEngine.getTemplateContext().setVariable("keys", Map.of("redis", "redisPassword"));
        spelTemplateEngine.getTemplateContext().setVariable("names", Map.of("redis", "redis-password"));
        spelTemplateEngine.getTemplateContext().setVariable("uris", Map.of("redis", "/mock/mySecret:redisPassword"));
    }

    @CsvSource(
        value = {
            "by name,           redis-password,    redis-password,     redisPassword,   false,  <<redis-password>>",
            "by uri,            null,              /mock/mySecret,     redisPassword,   false,  <</mock/mySecret:redisPassword>>",
            "by name with EL,   redis-password,    redis-password,     null,            true,   <<redis-password:#keys['redis']>>",
            "by uri with EL,    null,              /mock/mySecret,     null,            true,   <</mock/mySecret:#keys['redis']>>",
        },
        nullValues = "null"
    )
    @ParameterizedTest(name = "{0}")
    void should_call_service_using_fromGrant(
        String test,
        String specName,
        String naturalId,
        String key,
        boolean dynKeys,
        String refAsString
    ) {
        Spec spec = new Spec(null, specName, "/mock/mySecret", key, null, dynKeys, false, null, null, ENV_ID);
        specLifeCycleService.deploy(spec);
        shortAwait().untilAsserted(() -> assertThat(cache.get(ENV_ID, naturalId)).isPresent());

        DiscoveryContext context = new DiscoveryContext(
            UUID.randomUUID(),
            ENV_ID,
            RefParser.parse(refAsString),
            new DiscoveryLocation(new DiscoveryLocation.Definition("test", "123"))
        );
        boolean authorized = grantService.grant(context, spec);
        assertThat(authorized).isTrue();
        grantService.grant(context, spec);

        String el = Formatter.computeELFromStatic(context, ENV_ID);
        assertThat(spelTemplateEngine.getValue(el, String.class)).isEqualTo("redisadmin");
    }

    @CsvSource(
        value = {
            "by name,               redis-password,    redis-password,  <<name {#names['redis']}>>,   true",
            "by uri,                null,              /mock/mySecret,  <<uri {#uris['redis']}>>,     true",
            "by uri on the fly,     null,              null,  <<uri {#uris['redis']}>>,     false",
        },
        nullValues = "null"
    )
    @ParameterizedTest(name = "{0}")
    void should_call_service_using_fromELWith(String test, String specName, String naturalId, String refAsString, boolean createSpec) {
        DiscoveryContext context = new DiscoveryContext(
            UUID.randomUUID(),
            ENV_ID,
            RefParser.parse(refAsString),
            new DiscoveryLocation(new DiscoveryLocation.Definition("test", "123"))
        );

        if (createSpec) {
            Spec spec = new Spec(null, specName, "/mock/mySecret", "redisPassword", null, false, false, null, null, ENV_ID);
            specLifeCycleService.deploy(spec);
            shortAwait().untilAsserted(() -> assertThat(cache.get(ENV_ID, naturalId)).isPresent());
            boolean authorized = grantService.grant(context, spec);
            assertThat(authorized).isTrue();
            grantService.grant(context, spec);
        }

        String el = Formatter.computeELFromEL(context, ENV_ID);
        assertThat(spelTemplateEngine.getValue(el, String.class)).isEqualTo("redisadmin");
    }

    ConditionFactory shortAwait() {
        return await().pollDelay(0, TimeUnit.MILLISECONDS).pollInterval(20, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS);
    }
}
