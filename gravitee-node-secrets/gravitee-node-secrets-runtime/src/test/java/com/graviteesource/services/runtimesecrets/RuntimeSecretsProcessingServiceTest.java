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
package com.graviteesource.services.runtimesecrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.discovery.DefaultContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.DefinitionBrowserRegistry;
import com.graviteesource.services.runtimesecrets.el.ContextUpdater;
import com.graviteesource.services.runtimesecrets.el.engine.SecretSpelTemplateEngine;
import com.graviteesource.services.runtimesecrets.errors.SecretAccessDeniedException;
import com.graviteesource.services.runtimesecrets.errors.SecretProviderException;
import com.graviteesource.services.runtimesecrets.grant.DefaultGrantService;
import com.graviteesource.services.runtimesecrets.grant.GrantRegistry;
import com.graviteesource.services.runtimesecrets.providers.DefaultResolverService;
import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import com.graviteesource.services.runtimesecrets.spec.DefaultSpecLifecycleService;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import com.graviteesource.services.runtimesecrets.storage.SimpleOffHeapCache;
import io.gravitee.el.spel.SpelExpressionParser;
import io.gravitee.el.spel.SpelTemplateEngine;
import io.gravitee.node.api.secrets.runtime.discovery.*;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.providers.ResolverService;
import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.gravitee.node.secrets.plugin.mock.MockSecretProvider;
import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.security.util.InMemoryResource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RuntimeSecretsProcessingServiceTest {

    public static final String FOO_ENV_ID = "foo";
    public static final String BAR_ENV_ID = "bar";
    InMemoryResource providerAllEnv = new InMemoryResource(
        """
            secrets:
              mySecret:
                  redisPassword: "fighters"
                  ldapPassword: "dog"
              flaky:
                  password: iamflaky
            errors:
                - secret: flaky
                  message: huge error!!!
                  repeat: 1
                - secret: error
                  message: I am not in the mood
                                                                         
            """
    );
    InMemoryResource providerBarEnv = new InMemoryResource(
        """
                secrets:
                  mySecret:
                      redisPassword: "tender"
                      ldapPassword: "regular"
                                                                                            
                """
    );
    private SpecLifecycleService specLifeCycleService;
    private Cache cache;
    private SpelTemplateEngine spelTemplateEngine;
    private RuntimeSecretsProcessingService cut;

    @BeforeEach
    void before() {
        SecretProviderRegistry secretProviderRegistry = new SecretProviderRegistry();

        final YamlPropertiesFactoryBean allEnvSPConfig = new YamlPropertiesFactoryBean();
        allEnvSPConfig.setResources(providerAllEnv);
        secretProviderRegistry.register(
            "mock",
            new MockSecretProvider(new MockSecretProviderConfiguration((Map) new LinkedHashMap<>(allEnvSPConfig.getObject()))),
            null
        );

        final YamlPropertiesFactoryBean barEnvSPConfig = new YamlPropertiesFactoryBean();
        barEnvSPConfig.setResources(providerBarEnv);
        secretProviderRegistry.register(
            "mock-bar",
            new MockSecretProvider(new MockSecretProviderConfiguration((Map) new LinkedHashMap<>(barEnvSPConfig.getObject()))),
            BAR_ENV_ID
        );

        cache = new SimpleOffHeapCache();
        Config config = new Config(true, 200, true);
        GrantService grantService = new DefaultGrantService(new GrantRegistry(), config);
        SpecRegistry specRegistry = new SpecRegistry();
        ContextRegistry contextRegistry = new DefaultContextRegistry();
        ResolverService resolverService = new DefaultResolverService(secretProviderRegistry);
        specLifeCycleService = new DefaultSpecLifecycleService(specRegistry, contextRegistry, cache, resolverService, grantService, config);
        ContextUpdater contextUpdater = new ContextUpdater(cache, grantService, specLifeCycleService, specRegistry);
        spelTemplateEngine = new SecretSpelTemplateEngine(new SpelExpressionParser());
        // set up EL variables
        contextUpdater.addRuntimeSecretsService(spelTemplateEngine.getTemplateContext());
        spelTemplateEngine.getTemplateContext().setVariable("uris", Map.of("redis", "/mock/mySecret:redisPassword"));

        DefinitionBrowserRegistry browserRegistry = new DefinitionBrowserRegistry(List.of(new TestDefinitionBrowser()));
        cut = new RuntimeSecretsProcessingService(browserRegistry, contextRegistry, specRegistry, grantService, specLifeCycleService);
    }

    @Test
    void should_discover_and_resolve_secret_on_the_fly() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "<</mock/mySecret:redisPassword>>", "");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
    }

    @Test
    void should_discover_and_resolve_secret_on_the_fly_with_mixed_string() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "Redis password is: <</mock/mySecret:redisPassword>>!", "");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("Redis password is: fighters!");
    }

    @Test
    void should_discover_and_resolve_secret_on_the_fly_from_el() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "Redis password is: << uri {#uris['redis']} >>!", "");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("Redis password is: fighters!");
    }

    @Test
    void should_discover_and_get_secret() {
        final String name = "redis-password";
        Spec spec = new Spec(null, name, "/mock/mySecret", "redisPassword", null, false, false, null, null, FOO_ENV_ID);
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<<" + name + ">>", "");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
    }

    @Test
    void should_get_a_different_secret_in_two_different_env() {
        final String name = "redis-password";
        Spec fooSpec = new Spec(null, name, "/mock/mySecret", "redisPassword", null, false, false, null, null, FOO_ENV_ID);
        Spec barSpec = new Spec(null, name, "/mock-bar/mySecret", "redisPassword", null, false, false, null, null, BAR_ENV_ID);
        specLifeCycleService.deploy(fooSpec);
        specLifeCycleService.deploy(barSpec);

        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());
        awaitShortly().untilAsserted(() -> assertThat(cache.get(BAR_ENV_ID, name)).isPresent());

        FakeDefinition fooDefinition = new FakeDefinition("123", "<<" + name + ">>", "");
        cut.onDefinitionDeploy(FOO_ENV_ID, fooDefinition, Map.of("revision", "1"));
        assertThat(spelTemplateEngine.getValue(fooDefinition.getFirst(), String.class)).isEqualTo("fighters");

        FakeDefinition barDefinition = new FakeDefinition("123", "<<" + name + ">>", "");
        cut.onDefinitionDeploy(BAR_ENV_ID, barDefinition, Map.of("revision", "1"));
        assertThat(spelTemplateEngine.getValue(barDefinition.getFirst(), String.class)).isEqualTo("tender");
    }

    @Test
    void should_discover_secrets_in_two_locations() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "<</mock/mySecret:redisPassword>>", "<</mock/mySecret:ldapPassword>>");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getSecond(), String.class)).isEqualTo("dog");
    }

    @Test
    void should_discover_deny_access_to_second_secrets_due_to_ACLs() {
        Spec spec = new Spec(
            null,
            null,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, "/mock/mySecret")).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<</mock/mySecret:redisPassword>>", "<</mock/mySecret:redisPassword>>");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
        assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getSecond(), String.class))
            .isInstanceOf(SecretAccessDeniedException.class);
    }

    @Test
    void should_fail_getting_secret_secrets_after_spec_undeployed() {
        String name = "redis-password";
        Spec spec = new Spec(null, name, "/mock/mySecret", "redisPassword", null, false, false, null, null, FOO_ENV_ID);
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<<" + name + ">>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
        specLifeCycleService.undeploy(spec);
        assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class))
            .isInstanceOf(SecretAccessDeniedException.class);
    }

    @Test
    void should_fail_to_resolve_secret_on_the_fly_then_succeeds_after_secret_it_is_present() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "<< /mock/flaky:password>>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(cache.get(FOO_ENV_ID, "/mock/flaky")).isPresent().get().extracting(Entry::type).isEqualTo(Entry.Type.ERROR);
        assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class))
            .isInstanceOf(SecretProviderException.class)
            .hasMessageContaining("huge error!!!");

        // it should be there any milliseconds
        await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() ->
                assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).doesNotThrowAnyException()
            );
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("iamflaky");
    }

    @Test
    void should_get_a_different_secret_after_spec_update() {
        String name = "password";
        String specID = UUID.randomUUID().toString();
        Spec spec = new Spec(
            specID,
            name,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<<" + name + ">>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");

        Spec specV2 = new Spec(
            specID,
            name,
            "/mock/mySecret",
            "ldapPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(specV2);
        awaitShortly()
            .untilAsserted(() -> assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("dog"));
    }

    @Test
    void should_fail_getting_secret_when_acls_changes() {
        String name = "password";
        String specID = UUID.randomUUID().toString();
        Spec spec = new Spec(
            specID,
            name,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<<" + name + ">>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");

        Spec specV2 = new Spec(
            specID,
            name,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("second", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(specV2);
        awaitShortly()
            .untilAsserted(() ->
                assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class))
                    .isInstanceOf(SecretAccessDeniedException.class)
            );
    }

    @Test
    void should_fail_to_get_error_when_secret_provider_returns_error() {
        FakeDefinition fakeDefinition = new FakeDefinition("123", "<</mock/error:ignored>>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class))
            .isInstanceOf(SecretProviderException.class)
            .hasMessageContaining("I am not in the mood");
    }

    @Test
    void should_fail_to_get_secret_after_undeploy() {
        String name = "password";
        String specID = UUID.randomUUID().toString();
        Spec spec = new Spec(
            specID,
            name,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(spec);
        awaitShortly().untilAsserted(() -> assertThat(cache.get(FOO_ENV_ID, name)).isPresent());

        FakeDefinition fakeDefinition = new FakeDefinition("123", "<<" + name + ">>", null);
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");

        specLifeCycleService.undeploy(spec);

        assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class))
            .isInstanceOf(SecretAccessDeniedException.class);
    }

    @Test
    void should_go_from_on_the_fly_to_named_user_flow() {
        // on the fly
        FakeDefinition fakeDefinition = new FakeDefinition("123", "<</mock/mySecret:redisPassword>>", "<</mock/mySecret:redisPassword>>");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition, Map.of("revision", "1"));

        assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
        assertThat(spelTemplateEngine.getValue(fakeDefinition.getSecond(), String.class)).isEqualTo("fighters");

        // create spec to limit sage
        String specID = UUID.randomUUID().toString();
        Spec spec = new Spec(
            specID,
            null,
            "/mock/mySecret",
            "redisPassword",
            null,
            false,
            false,
            null,
            new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
            FOO_ENV_ID
        );
        specLifeCycleService.deploy(spec);
        awaitShortly()
            .untilAsserted(() -> {
                assertThat(spelTemplateEngine.getValue(fakeDefinition.getFirst(), String.class)).isEqualTo("fighters");
                assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition.getSecond(), String.class))
                    .isInstanceOf(SecretAccessDeniedException.class);
            });

        // create spec to limit sage
        spec =
            new Spec(
                specID,
                "redis-password",
                "/mock/mySecret",
                "redisPassword",
                null,
                false,
                false,
                null,
                new ACLs(null, List.of(new ACLs.PluginACL("first", null))),
                FOO_ENV_ID
            );
        specLifeCycleService.deploy(spec);

        FakeDefinition fakeDefinition2 = new FakeDefinition("123", "<<redis-password >>", "<< redis-password>>");
        cut.onDefinitionDeploy(FOO_ENV_ID, fakeDefinition2, Map.of("revision", "2"));

        awaitShortly()
            .untilAsserted(() -> {
                assertThat(cache.get(FOO_ENV_ID, "redis-password")).isPresent().get().extracting(Entry::type).isEqualTo(Entry.Type.VALUE);
                // TODO assert old secret is still there
                assertThat(spelTemplateEngine.getValue(fakeDefinition2.getFirst(), String.class)).isEqualTo("fighters");
                assertThatCode(() -> spelTemplateEngine.getValue(fakeDefinition2.getSecond(), String.class))
                    .isInstanceOf(SecretAccessDeniedException.class);
            });
        // TODO assert old secret is evict after undeploy of revision 1
    }

    @Test
    void should_continue_getting_secret_when_previous_revision_removed_unused_are_evicted() {
        // PARAMTERIZED => secret are [on the fly, named, uri]
        // simulate fake definition deploy
        // - deploy rev1 => secret 1 + secret 2
        // - deploy rev2 => secret 1
        // - undeploy rev1
        // => secret 1 still available
    }

    static class TestDefinitionBrowser implements DefinitionBrowser<FakeDefinition> {

        @Override
        public boolean canHandle(Object definition) {
            return definition instanceof FakeDefinition;
        }

        @Override
        public Definition getDefinitionKindLocation(FakeDefinition definition, Map<String, String> metadata) {
            return new Definition("test", definition.getId(), Optional.of(metadata.get("revision")));
        }

        @Override
        public void findPayloads(FakeDefinition definition, DefinitionPayloadNotifier notifier) {
            if (definition.getFirst() != null) {
                notifier.onPayload(definition.getFirst(), new PayloadLocation(PayloadLocation.PLUGIN_KIND, "first"), definition::setFirst);
            }
            if (definition.getSecond() != null) {
                notifier.onPayload(
                    definition.getSecond(),
                    new PayloadLocation(PayloadLocation.PLUGIN_KIND, "second"),
                    definition::setSecond
                );
            }
        }
    }

    ConditionFactory awaitShortly() {
        return await().pollDelay(0, TimeUnit.MILLISECONDS).pollInterval(20, TimeUnit.MILLISECONDS).atMost(100, TimeUnit.MILLISECONDS);
    }
}

@Data
@AllArgsConstructor
class FakeDefinition {

    private String id, first, second;
}
