package io.gravitee.node.secrets.internal.fake;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.secrets.SecretProviderPlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TestSecretProviderPlugin implements SecretProviderPlugin<TestSecretProviderFactory, TestSecretProviderConfiguration> {

    private final boolean deployed;

    public TestSecretProviderPlugin(boolean deployed) {
        this.deployed = deployed;
    }

    @Override
    public String id() {
        return "test";
    }

    @Override
    public String clazz() {
        return TestSecretProviderFactory.class.getName();
    }

    @Override
    public Class<TestSecretProviderFactory> secretProviderFactory() {
        return TestSecretProviderFactory.class;
    }

    @Override
    public Path path() {
        return Path.of("src/test/resources");
    }

    @Override
    public PluginManifest manifest() {
        return new PluginManifest() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public String name() {
                return "Test Secret Provider";
            }

            @Override
            public String description() {
                return "Test Secret Provider";
            }

            @Override
            public String category() {
                return "secret providers";
            }

            @Override
            public String version() {
                return "0.0.0";
            }

            @Override
            public String plugin() {
                return TestSecretProviderFactory.class.getName();
            }

            @Override
            public String type() {
                return SecretProvider.PLUGIN_TYPE;
            }

            @Override
            public String feature() {
                return null;
            }
        };
    }

    @Override
    public URL[] dependencies() {
        return new URL[0];
    }

    @Override
    public boolean deployed() {
        return this.deployed;
    }

    @Override
    public Class<TestSecretProviderConfiguration> configuration() {
        return null;
    }
}
