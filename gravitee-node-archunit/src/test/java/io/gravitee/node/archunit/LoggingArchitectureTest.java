package io.gravitee.node.archunit;

import java.util.Set;
import org.junit.Test;

/**
 * ArchUnit tests enforcing Gravitee logging policy using the shared fluent DSL
 * provided by {@link LoggingArchitectureRules}.
 *
 * <p>This test suite verifies that, within the selected base package(s):</p>
 * <ul>
 *   <li>Classes do not depend on SLF4J {@code org.slf4j.LoggerFactory} (interface {@code org.slf4j.Logger} remains allowed).</li>
 *   <li>Classes are not annotated with Lombok {@code @lombok.extern.slf4j.Slf4j}.</li>
 * </ul>
 *
 * <p>The base package to scan is explicitly provided by the test via
 * {@code resideInAnyPackage("io.gravitee.node..")}. No default is assumed by the DSL.</p>
 *
 * <p>An allow-list of fully qualified class names is supplied to exempt known and
 * intentional usages (e.g., bootstrapping classes manipulating the logging backend).</p>
 */
public class LoggingArchitectureTest {

    private static final Set<String> ALLOW_LIST = Set.of(
        // Requires LoggerFactory.getILoggerFactory() to override logback configuration
        "io.gravitee.node.container.AbstractContainer",
        // Factory class allowed referencing an underlying logging system
        "io.gravitee.node.logging.NodeLoggerFactory",
        // Requires LoggerFactory.getILoggerFactory() to override logback configuration
        "io.gravitee.node.management.http.node.log.LoggingEndpoint"
    );

    @Test
    public void no_dep_on_slf4j_except_allow_list() {
        LoggingArchitectureRules.configure().allowIn(ALLOW_LIST).resideInAnyPackage("io.gravitee.node..").checkNoSlf4jLoggerFactory();
    }
}
