package io.gravitee.node.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Fluent DSL to enforce Gravitee logging architecture rules using ArchUnit.
 *
 * <p>Rules enforced:</p>
 * <ul>
 *   <li>No direct dependency on SLF4J {@code org.slf4j.LoggerFactory}</li>
 *   <li>No usage of Lombok {@code @lombok.extern.slf4j.Slf4j}</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>SLF4J's {@code org.slf4j.Logger} interface is allowed.</li>
 *   <li>Test classes are excluded from the scanned classes.</li>
 *   <li>You MUST provide the packages to scan via {@link Builder#resideInAnyPackage(String...)}.</li>
 *   <li>You MAY provide an allow-list of fully qualified class names via {@link Builder#allowIn(Collection)}
 *       to temporarily exempt legacy classes.</li>
 * </ul>
 *
 * <p>Typical usage from a test:</p>
 * <pre>{@code
 * @Test
 * public void should_use_gravitee_logging() {
 *   LoggingArchitectureRules
 *     .configure()
 *     .allowIn(Set.of(
 *         "io.gravitee.node.container.AbstractContainer",
 *         "io.gravitee.node.logging.NodeLoggerFactory"
 *     ))
 *     .resideInAnyPackage("io.gravitee.node..")
 *     .checkAll();
 * }
 * }</pre>
 */
public final class LoggingArchitectureRules {

    private LoggingArchitectureRules() {}

    public static Builder configure() {
        return new Builder();
    }

    public static final class Builder {

        private static final String SLF4J_LOGGER_FACTORY = "org.slf4j.LoggerFactory";
        private static final String LOMBOK_SLF4J_ANNOTATION = "lombok.extern.slf4j.Slf4j";

        private final Set<String> allowList = new HashSet<>();
        private String[] basePackages;

        private Builder() {}

        /**
         * Adds a collection of class names to the allow-list. Classes in this allow-list are exempt
         * from the rules executed by this builder (useful for known legacy hotspots).
         *
         * @param allowList fully qualified class names to allow (null is ignored)
         * @return this builder
         */
        public Builder allowIn(Collection<String> allowList) {
            if (allowList != null) {
                this.allowList.addAll(allowList);
            }
            return this;
        }

        /**
         * Defines the packages to scan. Accepts ArchUnit pattern syntax (e.g. {@code "io.gravitee.node.."}).
         * This method is mandatory before calling any {@code check*} method.
         *
         * @param basePackages one or more package patterns
         * @return this builder
         */
        public Builder resideInAnyPackage(String... basePackages) {
            if (basePackages != null && basePackages.length > 0) {
                this.basePackages = basePackages;
            }
            return this;
        }

        /**
         * Checks the rule: no dependency on SLF4J {@code LoggerFactory}, except classes in the allow-list.
         */
        public void checkNoSlf4jLoggerFactory() {
            JavaClasses classes = importClasses();
            ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(this.basePackages)
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(SLF4J_LOGGER_FACTORY)
                .as("Classes must not depend on SLF4J LoggerFactory");

            rule.check(classes.that(notInAllowList()));
        }

        /**
         * Checks the rule: no Lombok {@code @Slf4j} annotation usage, except classes in the allow-list.
         */
        public void checkNoLombokSlf4j() {
            JavaClasses classes = importClasses();
            ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(this.basePackages)
                .should()
                .beAnnotatedWith(LOMBOK_SLF4J_ANNOTATION)
                .as("Classes must not be annotated with Lombok @Slf4j");

            rule.check(classes.that(notInAllowList()));
        }

        /**
         * Checks both rules: no {@code LoggerFactory} dependency and no Lombok {@code @Slf4j} usage.
         */
        public void checkAll() {
            checkNoSlf4jLoggerFactory();
            checkNoLombokSlf4j();
        }

        private JavaClasses importClasses() {
            if (this.basePackages == null || this.basePackages.length == 0) {
                throw new IllegalStateException(
                    "Base packages are not configured. Call resideInAnyPackage(String...) before running checks."
                );
            }
            return new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages(packagesRoot());
        }

        private String[] packagesRoot() {
            // ArchUnit's importer.importPackages accepts package names without the trailing ".." wildcard for root resolution.
            // To keep behavior consistent with the original tests, we pass the roots as-is and rely on ArchUnit patterns
            // set in the rules via .resideInAnyPackage(this.basePackages).
            // Here we derive package roots for import by stripping trailing dots when present.
            String[] roots = java.util.Arrays
                .stream(this.basePackages)
                .map(p -> java.util.Objects.toString(p, ""))
                // If a pattern ends with "..", strip the last two dots for importer root
                .map(p -> p.endsWith("..") ? p.substring(0, p.length() - 2) : p)
                .toArray(String[]::new);
            return roots;
        }

        private DescribedPredicate<JavaClass> notInAllowList() {
            return new DescribedPredicate<JavaClass>("not in allow-list") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return !allowList.contains(javaClass.getName());
                }
            };
        }
    }
}
