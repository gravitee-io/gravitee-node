package io.gravitee.node.secrets.plugin.mock;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MockSecretProviderTest {

    private SecretProvider cut;

    @BeforeEach
    void setup() {
        Map<String, Object> conf = Map.of("enabled", true, "secrets.redis.password", "r3d1s", "secrets.ldap.password", "1da9");
        this.cut = new MockSecretProviderFactory().create(new MockSecretProviderConfiguration(conf));
    }

    @Test
    void should_create_mount() {
        SecretMount secretMountRedis = cut.fromURL(SecretURL.from("secret://mock/redis:password"));
        assertThat(secretMountRedis.provider()).isEqualTo("mock");
        assertThat((String) secretMountRedis.location().get("secret")).isEqualTo("redis");
        assertThat(secretMountRedis.key()).isEqualTo("password");
    }

    @Test
    void should_resolve() {
        SecretMount secretMountRedis = cut.fromURL(SecretURL.from("secret://mock/redis:password"));
        cut.resolve(secretMountRedis).test().assertValue(SecretMap.of(Map.of("password", "r3d1s")));

        SecretMount secretMountLdap = cut.fromURL(SecretURL.from("secret://mock/ldap"));
        cut.resolve(secretMountLdap).test().assertValue(SecretMap.of(Map.of("password", "1da9")));
    }

    @Test
    void should_return_empty() {
        SecretMount secretMountEmpty = cut.fromURL(SecretURL.from("secret://mock/empty:password"));
        cut.resolve(secretMountEmpty).test().assertNoErrors().assertComplete();
    }
}
