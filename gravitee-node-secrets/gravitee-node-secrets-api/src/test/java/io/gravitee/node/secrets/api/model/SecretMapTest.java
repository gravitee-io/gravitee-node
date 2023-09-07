package io.gravitee.node.secrets.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretMapTest {

    static String KEY = "pass";
    static String SECRET = "foo";
    static String SECRET_B64 = "Zm9v";

    public static Stream<Arguments> secretMaps() {
        return Stream.of(
            Arguments.of("simple string", SecretMap.of(Map.of(KEY, SECRET))),
            Arguments.of("simple byte", SecretMap.of(Map.of(KEY, SECRET.getBytes(StandardCharsets.UTF_8)))),
            Arguments.of("simple string b64", SecretMap.ofBase64(Map.of(KEY, SECRET_B64))),
            Arguments.of("simple byte b64", SecretMap.ofBase64(Map.of(KEY, SECRET_B64.getBytes(StandardCharsets.UTF_8)))),
            Arguments.of("simple from map string", new SecretMap(Map.of(KEY, new Secret(SECRET))))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("secretMaps")
    void should_get_secret_from_map(String name, SecretMap secretMap) {
        SecretMount pass = new SecretMount(null, null, KEY, null);
        assertThat(secretMap.getSecret(pass)).isPresent().get().extracting(Secret::asString).isEqualTo(SECRET);
        assertThat(secretMap.getSecret(new SecretMount(null, null, "bar", null))).isNotPresent();
    }

    @Test
    void should_have_expireAt_set() {
        SecretMap secretMap = SecretMap.of(Map.of(KEY, SECRET));
        assertThat(secretMap.expireAt()).isNotPresent();
        secretMap = SecretMap.of(Map.of(KEY, SECRET), Instant.now());
        assertThat(secretMap.expireAt()).isPresent();
        secretMap = SecretMap.ofBase64(Map.of(KEY, SECRET), Instant.now());
        assertThat(secretMap.expireAt()).isPresent();
        secretMap = new SecretMap(Map.of(KEY, new Secret(SECRET)), Instant.now());
        assertThat(secretMap.expireAt()).isPresent();
    }

    @Test
    void should_have_well_know_data() {
        SecretMap secretMap = SecretMap.of(Map.of(KEY, SECRET));
        secretMap.handleWellKnownSecretKeys(Map.of(KEY, SecretMap.WellKnownSecretKey.PASSWORD));
        assertThat(secretMap.getSecret(new SecretMount(null, null, KEY, null)))
            .isPresent()
            .get()
            .extracting(Secret::asString)
            .isEqualTo(SECRET);
        assertThat(secretMap.wellKnown(SecretMap.WellKnownSecretKey.PASSWORD))
            .isPresent()
            .get()
            .extracting(Secret::asString)
            .isEqualTo(SECRET);
        assertThat(secretMap.wellKnown(SecretMap.WellKnownSecretKey.CERTIFICATE)).isNotPresent();
    }

    @Test
    void should_have_equals_and_hash_code() {
        assertThat(SecretMap.of(Map.of(KEY, SECRET))).isEqualTo(SecretMap.of(Map.of(KEY, SECRET)));
        assertThat(SecretMap.of(Map.of(KEY, SECRET))).hasSameHashCodeAs(SecretMap.of(Map.of(KEY, SECRET)));
        assertThat(SecretMap.of(Map.of(KEY, SECRET))).isNotEqualTo(SecretMap.ofBase64(Map.of(KEY, SECRET_B64)));
        assertThat(SecretMap.of(Map.of(KEY, SECRET)).hashCode()).isNotEqualTo(SecretMap.ofBase64(Map.of(KEY, SECRET_B64)).hashCode());
    }
}
