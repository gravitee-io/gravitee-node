package io.gravitee.node.secrets.api.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
class SecretURLTest {

    public static Stream<Arguments> workingURLs() {
        return Stream.of(
            Arguments.arguments("secret://foo/bar", "foo", List.of("bar"), Map.of(), false),
            Arguments.arguments("secret://foo/bar/", "foo", List.of("bar"), Map.of(), false),
            Arguments.arguments("secret://foo/bar//", "foo", List.of("bar"), Map.of(), false),
            Arguments.arguments("secret://foo/bar/puk", "foo", List.of("bar", "puk"), Map.of(), false),
            Arguments.arguments("secret://foo/bar/puk?", "foo", List.of("bar", "puk"), Map.of(), false),
            Arguments.arguments("secret://foo/bar/puk?watch", "foo", List.of("bar", "puk"), Map.of("watch", "true"), true),
            Arguments.arguments("secret://foo/bar/puk?watch=true", "foo", List.of("bar", "puk"), Map.of("watch", "true"), true),
            Arguments.arguments("secret://foo/bar/puk?watch=True", "foo", List.of("bar", "puk"), Map.of("watch", "True"), true),
            Arguments.arguments(
                "secret://foo/bar/puk?watch=true&beer",
                "foo",
                List.of("bar", "puk"),
                Map.of("watch", "true", "beer", "true"),
                true
            ),
            Arguments.arguments("secret://foo/bar/puk?watch=false", "foo", List.of("bar", "puk"), Map.of("watch", "false"), false),
            Arguments.arguments(
                "secret://foo/bar/puk?watch=false&order=7",
                "foo",
                List.of("bar", "puk"),
                Map.of("watch", "false", "order", "7"),
                false
            )
        );
    }

    @ParameterizedTest
    @MethodSource("workingURLs")
    void shouldParseURL(String url, String provider, List<String> paths, Map<String, String> query, boolean watch) {
        SecretURL cut = SecretURL.from(url);
        assertThat(cut.provider()).isEqualTo(provider);
        assertThat(cut.pathAsList()).containsExactlyElementsOf(paths);
        assertThat(cut.query()).containsAllEntriesOf(query);
        assertThat(cut.isWatchable()).isEqualTo(watch);
    }

    public static Stream<Arguments> nonWorkingURLs() {
        return Stream.of(
            Arguments.arguments("hey://foo/bar", "should have the following format"),
            Arguments.arguments("secret:/foo", "should have the following format"),
            Arguments.arguments("secret://foo", "should have the following format"),
            Arguments.arguments("secret://foo/", "should have the following format"),
            Arguments.arguments("secret://foo?", "should have the following format"),
            Arguments.arguments("secret://foo/?", "should have the following format"),
            Arguments.arguments("secret://foo/ /?", "contains spaces-only url parts"),
            Arguments.arguments("secret://foo/ /bar?", "contains spaces-only url parts"),
            Arguments.arguments("secret://foo//bar?", "contains spaces-only url parts")
        );
    }

    @ParameterizedTest
    @MethodSource("nonWorkingURLs")
    void shouldNotParseURL(String url, String message) {
        assertThatCode(() -> SecretURL.from(url)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(message);
    }
}
