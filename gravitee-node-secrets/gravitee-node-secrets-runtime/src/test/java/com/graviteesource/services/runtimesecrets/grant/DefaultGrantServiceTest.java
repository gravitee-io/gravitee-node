package com.graviteesource.services.runtimesecrets.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.graviteesource.services.runtimesecrets.config.Config;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryLocation;
import io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultGrantServiceTest {

    private DefaultGrantService cut;

    @BeforeEach
    void setup() {
        Config config = new Config(true, true);
        this.cut = new DefaultGrantService(new GrantRegistry(), config);
    }

    public static Stream<Arguments> grants() {
        return Stream.of(
            arguments("null spec", context("dev", "api", "123"), null, true, "no spec found"),
            arguments("no acl same env", context("dev", "api", "123"), spec("dev", null), true, null),
            arguments("empty acl same env", context("dev", "api", "123"), spec("dev", new ACLs(List.of(), List.of())), true, null),
            arguments("no acl diff env", context("dev", "api", "123"), spec("test", null), false, null),
            arguments(
                "def acl ok",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), null)),
                true,
                null
            ),
            arguments(
                "def acl wrong id",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("456"))), null)),
                false,
                null
            ),
            arguments(
                "def acl wrong kind",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("dict", List.of("123"))), null)),
                false,
                null
            ),
            arguments(
                "def acl many",
                context("dev", "api", "123"),
                spec(
                    "dev",
                    new ACLs(
                        List.of(new ACLs.DefinitionACL("dict", List.of("123")), new ACLs.DefinitionACL("api", List.of("123", "456"))),
                        null
                    )
                ),
                true,
                null
            ),
            arguments(
                "plugin acl ok",
                context("dev", "api", "123", plugin("foo")),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), List.of(new ACLs.PluginACL("foo", null)))),
                true,
                null
            ),
            arguments(
                "plugin acl ko",
                context("dev", "api", "123", plugin("foo")),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), List.of(new ACLs.PluginACL("bar", null)))),
                false,
                null
            ),
            arguments(
                "plugin acl ok many",
                context("dev", "api", "123", plugin("foo")),
                spec(
                    "dev",
                    new ACLs(
                        List.of(new ACLs.DefinitionACL("api", List.of("123"))),
                        List.of(new ACLs.PluginACL("bar", null), new ACLs.PluginACL("foo", null))
                    )
                ),
                true,
                null
            )
        );
    }

    @MethodSource("grants")
    @ParameterizedTest(name = "{0}")
    void should_authorize(String name, DiscoveryContext context, Spec spec, boolean granted, String error) {
        if (error != null) {
            assertThatCode(() -> cut.authorize(context, spec)).hasMessageContaining(error);
        } else {
            assertThat(cut.authorize(context, spec)).isEqualTo(granted);
        }
    }

    static DiscoveryContext context(String env, String kind, String id, PayloadLocation... payloads) {
        return new DiscoveryContext(
            null,
            env,
            new Ref(Ref.MainType.NAME, new Ref.Expression("secret", false), null, null, "<< secret >>"),
            new DiscoveryLocation(new DiscoveryLocation.Definition(kind, id), payloads)
        );
    }

    static Spec spec(String env, ACLs acls) {
        return new Spec(null, "secret", null, null, null, false, false, null, acls, env);
    }

    static PayloadLocation plugin(String id) {
        return new PayloadLocation(PayloadLocation.PLUGIN_KIND, id);
    }
}
