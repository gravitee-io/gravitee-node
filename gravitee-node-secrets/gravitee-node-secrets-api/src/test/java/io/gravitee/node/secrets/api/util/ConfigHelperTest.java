package io.gravitee.node.secrets.api.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.node.secrets.api.model.Secret;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
class ConfigHelperTest {

    static Stream<Arguments> properties() {
        return Stream.of(
            Arguments.of("empty", Map.of(), "foo", List.of()),
            Arguments.of("single value", Map.of("foo.bar", true), "foo", List.of("bar")),
            Arguments.of("two values", Map.of("foo.bar", true, "foo.joe", true), "foo", List.of("bar", "joe")),
            Arguments.of("no match", Map.of("foo.bar", true), "bar", List.of()),
            Arguments.of("partial match", Map.of("foo.bar", true, "puz.joe", true), "foo", List.of("bar"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("properties")
    void should_chop_properties(String _testName, Map<String, Object> properties, String prefix, List<String> rest) {
        var chopped = ConfigHelper.removePrefix(properties, prefix);
        assertThat(chopped.keySet()).containsExactlyInAnyOrderElementsOf(rest);
        assertThat(chopped.values()).allMatch(v -> Objects.equals(v, Boolean.TRUE));
    }

    @Test
    void should_find_value_in_enum() {
        TestEnum a = ConfigHelper.enumValueOfIgnoreCase("a", TestEnum.class, null);
        assertThat(a).isSameAs(TestEnum.A);
        TestEnum A = ConfigHelper.enumValueOfIgnoreCase("A", TestEnum.class, null);
        assertThat(A).isSameAs(TestEnum.A);
        TestEnum b = ConfigHelper.enumValueOfIgnoreCase("b", TestEnum.class, null);
        assertThat(b).isSameAs(TestEnum.b);
        TestEnum B = ConfigHelper.enumValueOfIgnoreCase("B", TestEnum.class, null);
        assertThat(B).isSameAs(TestEnum.b);
        assertThatCode(() -> ConfigHelper.enumValueOfIgnoreCase("c", TestEnum.class, "foo.bar.baz.puk"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("foo.bar.baz.puk");
    }

    @Test
    void should_get_secret_or_string() {
        Map<String, Object> properties = Map.of("foo", "bar", "foo_secret", new Secret("bar"));
        assertThat(ConfigHelper.getStringOrSecret(properties, "foo")).isEqualTo("bar");
        assertThat(ConfigHelper.getStringOrSecret(properties, "foo_secret")).isEqualTo("bar");
        assertThat(ConfigHelper.getStringOrSecret(properties, "puk", "yeah")).isEqualTo("yeah");
    }

    public enum TestEnum {
        A,
        b,
    }
}
