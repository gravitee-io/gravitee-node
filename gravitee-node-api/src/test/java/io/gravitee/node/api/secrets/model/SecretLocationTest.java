package io.gravitee.node.api.secrets.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretLocationTest {

    @Test
    void should_get() {
        SecretLocation secretLocation = new SecretLocation(Map.of("foo", "bar"));
        assertThat((String) secretLocation.get("foo")).isEqualTo("bar");
        assertThat((String) secretLocation.get("fuu")).isNull();
    }

    @Test
    void should_put_in_existing() {
        SecretLocation secretLocation = new SecretLocation(Map.of("foo", "bar"));
        secretLocation.put("hello", "world");
        assertThat((String) secretLocation.get("hello")).isEqualTo("world");
        assertThat((String) secretLocation.get("foo")).isEqualTo("bar");
    }

    @Test
    void should_put_in_empty() {
        SecretLocation secretLocation = new SecretLocation();
        secretLocation.put("hello", "world");
        assertThat((String) secretLocation.get("hello")).isEqualTo("world");
    }

    @Test
    void should_getOrDefault() {
        SecretLocation secretLocation = new SecretLocation();
        assertThat((String) secretLocation.get("foo")).isNull();
        assertThat((String) secretLocation.getOrDefault("foo", "bar")).isEqualTo("bar");
    }

    @Test
    void should_fail() {
        SecretLocation secretLocation = new SecretLocation();
        assertThatCode(() -> secretLocation.get(null)).isInstanceOf(NullPointerException.class);
        assertThatCode(() -> secretLocation.put(null, "bar")).isInstanceOf(NullPointerException.class);
        assertThatCode(() -> secretLocation.getOrDefault(null, "bar")).isInstanceOf(NullPointerException.class);
    }
}
