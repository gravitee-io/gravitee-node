package io.gravitee.node.secrets.api.model;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.secrets.api.util.ConfigHelper;
import java.security.cert.CollectionCertStoreParameters;
import java.util.*;
import java.util.stream.Stream;
import jdk.jfr.Name;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretURLTest {

    static Stream<Arguments> workingURLs() {
        return Stream.of(
            arguments("secret://foo/bar", "foo", List.of("bar"), Map.of(), false),
            arguments("secret://foo/bar/", "foo", List.of("bar"), Map.of(), false),
            arguments("secret://foo/bar//", "foo", List.of("bar"), Map.of(), false),
            arguments("secret://foo/bar/puk", "foo", List.of("bar", "puk"), Map.of(), false),
            arguments("secret://foo/bar/puk?", "foo", List.of("bar", "puk"), Map.of(), false),
            arguments("secret://foo/bar/puk?watch", "foo", List.of("bar", "puk"), Map.of("watch", List.of("true")), true),
            arguments("secret://foo/bar/puk?watch=true", "foo", List.of("bar", "puk"), Map.of("watch", List.of("true")), true),
            arguments("secret://foo/bar/puk?watch=True", "foo", List.of("bar", "puk"), Map.of("watch", List.of("True")), true),
            arguments(
                "secret://foo/bar/puk?watch=true&beer",
                "foo",
                List.of("bar", "puk"),
                Map.of("watch", List.of("true"), "beer", List.of("true")),
                true
            ),
            arguments("secret://foo/bar/puk?watch=false", "foo", List.of("bar", "puk"), Map.of("watch", List.of("false")), false),
            arguments(
                "secret://foo/bar/puk?watch=false&exclude=7&exclude=9",
                "foo",
                List.of("bar", "puk"),
                Map.of("watch", List.of("false"), "exclude", List.of("7", "9")),
                false
            )
        );
    }

    @ParameterizedTest
    @MethodSource("workingURLs")
    void should_parse_url(String url, String provider, List<String> paths, Map<String, Collection<String>> query, boolean watch) {
        SecretURL cut = SecretURL.from(url);
        assertThat(cut.provider()).isEqualTo(provider);
        assertThat(cut.pathAsList()).containsExactlyElementsOf(paths);
        assertThat(cut.query().asMap()).containsAllEntriesOf(query);
        assertThat(cut.isWatchable()).isEqualTo(watch);
    }

    static Stream<Arguments> nonWorkingURLs() {
        return Stream.of(
            arguments("hey://foo/bar", "should have the following format"),
            arguments("secret:/foo", "should have the following format"),
            arguments("secret://foo", "should have the following format"),
            arguments("secret://foo/", "should have the following format"),
            arguments("secret://foo?", "should have the following format"),
            arguments("secret://foo/?", "should have the following format"),
            arguments("secret://foo/ /?", "contains spaces-only url parts"),
            arguments("secret://foo/ /bar?", "contains spaces-only url parts"),
            arguments("secret://foo//bar?", "contains spaces-only url parts")
        );
    }

    @ParameterizedTest
    @MethodSource("nonWorkingURLs")
    void should_not_parse_url(String url, String message) {
        assertThatCode(() -> SecretURL.from(url)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
    }

    public static Stream<Arguments> wellKnowKeysURLs() {
        return Stream.of(
            arguments(
                "secret://foo/bar?keymap=certificate:tls.crt&keymap=private_key:tls.key",
                Map.of("tls.crt", SecretMap.WellKnownSecretKey.CERTIFICATE, "tls.key", SecretMap.WellKnownSecretKey.PRIVATE_KEY)
            ),
            arguments(
                "secret://foo/bar?keymap=username:user&keymap=password:passwd",
                Map.of("user", SecretMap.WellKnownSecretKey.USERNAME, "passwd", SecretMap.WellKnownSecretKey.PASSWORD)
            ),
            arguments(
                "secret://foo/bar?keymap=certificate:tls.crt&keymap=key:tls.key",
                Map.of("tls.crt", SecretMap.WellKnownSecretKey.CERTIFICATE)
            ),
            arguments(
                "secret://foo/bar?keymap=cert:tls.crt&keymap=private_key:tls.key",
                Map.of("tls.key", SecretMap.WellKnownSecretKey.PRIVATE_KEY)
            ),
            arguments("secret://foo/bar?keymap=private_key:tls.key", Map.of("tls.key", SecretMap.WellKnownSecretKey.PRIVATE_KEY)),
            arguments("secret://foo/bar?keymap=foo:tls.crt&keymap=bar:tls.key", Map.of()),
            arguments("secret://foo/bar?keymap=foo&keymap=bar", Map.of())
        );
    }

    @ParameterizedTest
    @MethodSource("wellKnowKeysURLs")
    void should_have_well_known_mapping(String url, Map<String, SecretMap.WellKnownSecretKey> expected) {
        SecretURL cut = SecretURL.from(url);
        assertThat(cut.wellKnowKeyMap()).containsAllEntriesOf(expected);
    }

    public static Stream<Arguments> failingWellKnownKeysURLs() {
        return Stream.of(
            arguments("secret://foo/bar?keymap=certificate:&keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap=certificate: &keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap=:tls.key&keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap= :tls.key&keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap=:&keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap=: &keymap=private_key:foo"),
            arguments("secret://foo/bar?keymap= : &keymap=private_key:foo")
        );
    }

    @ParameterizedTest
    @MethodSource("failingWellKnownKeysURLs")
    void should_fail_parsing_well_known_mapping_error(String url) {
        SecretURL cut = SecretURL.from(url);
        assertThatThrownBy(cut::wellKnowKeyMap).isInstanceOf(IllegalArgumentException.class);
    }
}
