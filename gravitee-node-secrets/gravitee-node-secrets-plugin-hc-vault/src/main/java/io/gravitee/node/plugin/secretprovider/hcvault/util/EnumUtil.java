package io.gravitee.node.plugin.secretprovider.hcvault.util;

import java.util.EnumSet;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EnumUtil {

    public static <T extends Enum<T>> T valueOfCaseInsensitive(String configParamName, String literal, Class<T> enumClass) {
        EnumSet<T> values = EnumSet.allOf(enumClass);

        for (T value : values) {
            if (Objects.equals(value.name().toUpperCase(), literal.toUpperCase())) {
                return value;
            }
        }
        throw new IllegalArgumentException(
            "Invalid value for %s: %s. Possible values: '%s'".formatted(
                    configParamName,
                    literal,
                    values.stream().map(Enum::name).map(String::toLowerCase).collect(Collectors.joining("', '"))
                )
        );
    }
}
