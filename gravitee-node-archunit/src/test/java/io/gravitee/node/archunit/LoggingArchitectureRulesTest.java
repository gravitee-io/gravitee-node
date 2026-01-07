package io.gravitee.node.archunit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class LoggingArchitectureRulesTest {

    @Test
    void should_exclude_packages() {
        LoggingArchitectureRules
            .configure()
            .includeTests(true)
            .resideInAnyPackage("io.gravitee.node.archunit.test..")
            .excludePackagesFromScan("io.gravitee.node.archunit.test.excluded..")
            .checkNoSlf4jLoggerFactory();
    }

    @Test
    void should_fail_when_package_has_not_been_excluded() {
        AssertionError assertionError = Assertions.assertThrows(
            AssertionError.class,
            () -> {
                LoggingArchitectureRules
                    .configure()
                    .includeTests(true)
                    .resideInAnyPackage("io.gravitee.node.archunit.test..")
                    .checkNoSlf4jLoggerFactory();
            }
        );
        assertThat(assertionError.getMessage())
            .contains("Classes must not depend on SLF4J LoggerFactory")
            .contains("io.gravitee.node.archunit.test.excluded.ExcludedClass");
    }
}
