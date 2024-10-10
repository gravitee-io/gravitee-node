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
package com.graviteesource.services.runtimesecrets.spec;

import static org.assertj.core.api.Assertions.assertThat;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.config.OnTheFlySpecs;
import com.graviteesource.services.runtimesecrets.config.Renewal;
import com.graviteesource.services.runtimesecrets.discovery.DefaultContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.RefParser;
import com.graviteesource.services.runtimesecrets.grant.DefaultGrantService;
import com.graviteesource.services.runtimesecrets.grant.GrantRegistry;
import com.graviteesource.services.runtimesecrets.providers.DefaultResolverService;
import com.graviteesource.services.runtimesecrets.providers.SecretProviderRegistry;
import com.graviteesource.services.runtimesecrets.renewal.RenewalService;
import com.graviteesource.services.runtimesecrets.storage.SimpleOffHeapCache;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import io.gravitee.node.secrets.plugin.mock.MockSecretProvider;
import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.security.util.InMemoryResource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSpecLifecycleServiceTest {

    public static final String ENV_ID = "foo";
    InMemoryResource inMemoryResource = new InMemoryResource(
        """
              secrets:
                mySecret:
                    redisPassword: "redisadmin"
                    ldapPassword: "ldapadmin"
                                                                                          
                """
    );

    SpecLifecycleService cut;
    private SimpleOffHeapCache cache;

    @BeforeEach
    void before() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(inMemoryResource);
        SecretProviderRegistry registry = new SecretProviderRegistry();
        registry.register(
            "mock",
            new MockSecretProvider(new MockSecretProviderConfiguration((Map) new LinkedHashMap<>(yaml.getObject()))),
            null
        );
        cache = new SimpleOffHeapCache();
        Config config = new Config(false, new OnTheFlySpecs(true, Duration.ZERO), new Renewal(true, Duration.ZERO));
        RenewalService renewalService = new RenewalService(null, cache, config);
        cut =
            new DefaultSpecLifecycleService(
                new SpecRegistry(),
                new DefaultContextRegistry(),
                cache,
                new DefaultResolverService(registry),
                new DefaultGrantService(new GrantRegistry(), config),
                renewalService,
                config
            );
    }

    @Test
    void should_deploy_spec_and_get_secret_map_from_cache() {
        Spec spec = new Spec(null, "redis-password", "/mock/mySecret", "redisPassword", null, false, false, null, null, ENV_ID);
        cut.deploy(spec);
        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> checkInCache("redis-password"));
    }

    @Test
    void should_deploy_spec_on_the_fly_then_get_secret_map() {
        Ref ref = RefParser.parse("<</mock/mySecret:redisPassword>>");
        assertThat(cut.shouldDeployOnTheFly(ref)).isTrue();
        Spec spec = cut.deployOnTheFly(ENV_ID, ref);
        assertThat(spec.uri()).isEqualTo("/mock/mySecret");
        assertThat(spec.key()).isEqualTo("redisPassword");
        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> checkInCache("/mock/mySecret"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "<<foo>>", "<<uri {#context.attributes['foo']}>>" })
    void should_not_deploy_on_the_fly(String s) {
        assertThat(cut.shouldDeployOnTheFly(RefParser.parse(s))).isFalse();
    }

    @Test
    void should_deploy_spec_and_get_secret_map_from_cache_un_deploy_check_cache_empty() {
        Spec spec = new Spec(null, "redis-password", "/mock/mySecret", "redisPassword", null, false, false, null, null, ENV_ID);
        cut.deploy(spec);
        Awaitility.await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> checkInCache("redis-password"));
        cut.undeploy(spec);
        assertThat(cache.get(ENV_ID, "redis-password")).isNotPresent();
    }

    private void checkInCache(String natualId) {
        Optional<Entry> foo = cache.get(ENV_ID, natualId);
        assertThat(foo).get().extracting(Entry::type).asString().isEqualTo("VALUE");
        assertThat(foo)
            .get()
            .extracting(Entry::value)
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("redisPassword", new Secret("redisadmin"))
            .containsEntry("ldapPassword", new Secret("ldapadmin"));
    }
}
