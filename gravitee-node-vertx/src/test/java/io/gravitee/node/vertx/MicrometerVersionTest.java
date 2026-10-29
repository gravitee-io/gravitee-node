package io.gravitee.node.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * micrometer-core and micrometer-commons must not be downgraded by vertx-micrometer-metrics
 * (versions before 1.15.12 are affected by known denial-of-service vulnerabilities).
 */
class MicrometerVersionTest {

    private static final String MINIMUM = "1.15.12";

    @Test
    void should_resolve_micrometer_artifacts_at_the_fixed_version_or_later() {
        assertThat(compare(version(MeterRegistry.class), MINIMUM)).isGreaterThanOrEqualTo(0);
        assertThat(compare(version(KeyValue.class), MINIMUM)).isGreaterThanOrEqualTo(0);
        assertThat(compare(version(PrometheusMeterRegistry.class), MINIMUM)).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_compare_versions_numerically() {
        assertThat(compare("1.15.9", MINIMUM)).isNegative();
        assertThat(compare("1.9.17", MINIMUM)).isNegative();
        assertThat(compare("1.15.12", MINIMUM)).isZero();
        assertThat(compare("1.16.0", MINIMUM)).isPositive();
    }

    private static int compare(String actual, String expected) {
        var a = numericParts(actual);
        var e = numericParts(expected);
        for (int i = 0; i < Math.max(a.length, e.length); i++) {
            int result = Integer.compare(i < a.length ? a[i] : 0, i < e.length ? e[i] : 0);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static int[] numericParts(String version) {
        return Arrays.stream(version.split("[.-]"))
            .takeWhile(part -> part.matches("\\d+"))
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    private static String version(Class<?> type) {
        var version = type.getPackage().getImplementationVersion();
        assertThat(version).as("implementation version of %s", type.getName()).isNotNull();
        return version;
    }
}
