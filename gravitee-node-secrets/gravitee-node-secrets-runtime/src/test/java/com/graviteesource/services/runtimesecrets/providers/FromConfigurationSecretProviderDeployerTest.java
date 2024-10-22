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
package com.graviteesource.services.runtimesecrets.providers;

import com.graviteesource.services.runtimesecrets.providers.config.FromConfigurationSecretProviderDeployer;
import com.graviteesource.services.runtimesecrets.testsupport.PluginManagerHelper;
import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.secrets.plugin.mock.MockSecretProvider;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.util.InMemoryResource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FromConfigurationSecretProviderDeployerTest {

    InMemoryResource inMemoryResource = new InMemoryResource(
        """
            api:
               secrets:
                 providers:
                   - plugin: "mock"
                     environments:
                       - "dev"
                     configuration:
                        enabled: true
                        secrets:
                          mySecret:
                            redisPassword: "foo"
                            ldapPassword: "bar"
                   - id: "all-env-secret-manager"
                     plugin: "mock"
                     configuration:
                        enabled: true
                        secrets:
                          my_secret:
                            redisPassword: "very-long-password"
                            ldapPassword: "also-quite-not-short-password"
                   - id: "disabled"
                     plugin: "mock"
                     configuration:
                       enabled: false
                                        
            """
    );
    private SecretProviderRegistry registry;
    private FromConfigurationSecretProviderDeployer cut;

    @BeforeEach
    void before() {
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(inMemoryResource);
        MockEnvironment mockEnvironment = new MockEnvironment();
        mockEnvironment.getPropertySources().addFirst(new MapPropertySource("test", new LinkedHashMap(yaml.getObject())));
        registry = new SecretProviderRegistry();
        SecretProviderPluginManager pluginManager = PluginManagerHelper.newPluginManagerWithMockPlugin();
        cut = new FromConfigurationSecretProviderDeployer(mockEnvironment, registry, pluginManager);
    }

    @Test
    void should_load_providers() {
        cut.init();
        AtomicReference<SecretProvider> last = new AtomicReference<>();
        registry
            .get("bar", "all-env-secret-manager")
            .test()
            .awaitCount(1)
            .assertValue(sp -> {
                last.set(sp);
                return sp instanceof MockSecretProvider;
            });
        registry
            .get("foo", "all-env-secret-manager")
            .test()
            .awaitCount(1)
            .assertValue(sp -> sp instanceof MockSecretProvider && sp == last.get());
        registry.get("dev", "mock").test().awaitCount(1).assertValue(sp -> sp instanceof MockSecretProvider && last.get() != sp);
        registry
            .get("test", "mock")
            .flatMapMaybe(sp -> sp.resolve(new SecretMount("mock", null, "", null, false)))
            .test()
            .assertError(err -> err.getMessage().contains("for provider: 'mock'"));
        registry
            .get("any", "disabled")
            .flatMapMaybe(sp -> sp.resolve(new SecretMount("mock", null, "", null, false)))
            .test()
            .assertError(err -> err.getMessage().contains("for provider: 'mock'"));
    }
}