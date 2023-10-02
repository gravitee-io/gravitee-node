package io.gravitee.node.secrets.service.resolver;

import static io.gravitee.node.secrets.service.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GraviteeConfigurationSecretPropertyResolverTest {

    private GraviteeConfigurationSecretPropertyResolver cut;

    @BeforeEach
    void before() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        env.setProperty("secrets.test.secrets.pass", "theMostPowerfulPasswordInTheWorldMate!!!");
        GraviteeConfigurationSecretResolverDispatcher dispatcher = newDispatcher(pluginManager, env);
        this.cut = new GraviteeConfigurationSecretPropertyResolver(dispatcher);
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "secret://test/test:pass,true",
            "kubernetes://test/test:pass,false",
            "foo://test/test:pass,false",
            "foo://test/test:pass,false",
            ", false",
            "'',false",
        }
    )
    void should_supports_or_not(String url, boolean supports) {
        assertThat(cut.supports(url)).isEqualTo(supports);
    }

    @ParameterizedTest
    @CsvSource(
        value = {
            "secret://test/test:pass,false",
            "secret://test/test:pass?watch,true",
            "secret://test/test:pass?watch=true,true",
            "secret://test/test:pass?watch=false,false",
        }
    )
    void should_be_watchable_or_not(String url, boolean watchable) {
        assertThat(cut.isWatchable(url)).isEqualTo(watchable);
    }

    @Test
    void should_resolve() {
        Secret secret = cut.resolve("secret://test/test:pass").blockingGet();
        assertThat(secret).isNotNull();
        assertThat(secret.asString()).isEqualTo("theMostPowerfulPasswordInTheWorldMate!!!");
    }

    @Test
    void should_watch() {
        Secret secret = cut.watch("secret://test/test:pass").blockingFirst();
        assertThat(secret).isNotNull();
        assertThat(secret.asString()).isEqualTo("theMostPowerfulPasswordInTheWorldMate!!!");
    }
}
