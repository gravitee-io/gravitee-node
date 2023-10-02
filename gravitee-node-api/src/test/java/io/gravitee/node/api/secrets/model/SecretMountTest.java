package io.gravitee.node.api.secrets.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretMountTest {

    @Test
    void should_know_if_key_is_empty() {
        SecretMount secretMount = new SecretMount(null, null, null, null);
        assertThat(secretMount.isKeyEmpty()).isTrue();
        secretMount = new SecretMount(null, null, "", null);
        assertThat(secretMount.isKeyEmpty()).isTrue();
        secretMount = new SecretMount(null, null, "foo", null);
        assertThat(secretMount.isKeyEmpty()).isFalse();
    }
}
