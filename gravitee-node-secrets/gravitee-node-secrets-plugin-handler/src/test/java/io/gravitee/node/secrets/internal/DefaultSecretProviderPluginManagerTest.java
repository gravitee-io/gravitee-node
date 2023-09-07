package io.gravitee.node.secrets.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.secrets.internal.fake.TestSecretProviderConfiguration;
import io.gravitee.node.secrets.internal.fake.TestSecretProviderFactory;
import io.gravitee.node.secrets.internal.fake.TestSecretProviderPlugin;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultSecretProviderPluginManagerTest {

    final DefaultSecretProviderPluginManager cut = new DefaultSecretProviderPluginManager(
        new DefaultSecretProviderClassLoaderFactory(),
        new PluginConfigurationHelper(null, null)
    );

    @Test
    void should_get_deployed_factory() {
        List<String> called = new ArrayList<>();
        cut.setOnNewPluginCallback(called::add);
        cut.register(
            new DefaultSecretProviderPlugin<>(
                new TestSecretProviderPlugin(true),
                TestSecretProviderFactory.class,
                TestSecretProviderConfiguration.class
            )
        );
        assertThat(called).containsExactly("test");
        assertThat((SecretProviderFactory) cut.getFactoryById("test")).isNotNull();
    }

    @Test
    void should_get_undeployed_factory() {
        cut.register(
            new DefaultSecretProviderPlugin<>(
                new TestSecretProviderPlugin(false),
                TestSecretProviderFactory.class,
                TestSecretProviderConfiguration.class
            )
        );
        assertThat((SecretProviderFactory) cut.getFactoryById("test")).isNull();
        assertThat((SecretProviderFactory) cut.getFactoryById("test", true)).isNotNull();
    }
}
