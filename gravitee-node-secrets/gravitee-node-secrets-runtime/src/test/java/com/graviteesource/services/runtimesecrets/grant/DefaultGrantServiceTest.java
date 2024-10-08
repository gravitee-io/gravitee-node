/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graviteesource.services.runtimesecrets.grant;

import static org.assertj.core.api.Assertions.assertThat;
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
        Config config = new Config(true, 0, true);
        this.cut = new DefaultGrantService(new GrantRegistry(), config);
    }

    public static Stream<Arguments> grants() {
        return Stream.of(
            arguments("no acl same env", context("dev", "api", "123"), spec("dev", null, null)),
            arguments("no acl same key", context("dev", "api", "123", "pwd"), spec("dev", null, "pwd")),
            arguments("empty acl same env", context("dev", "api", "123"), spec("dev", new ACLs(List.of(), List.of()), null)),
            arguments(
                "def acl ok",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), null), null)
            ),
            arguments(
                "def acl ok same key",
                context("dev", "api", "123", "pwd"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), null), "pwd")
            ),
            arguments(
                "def acl ok many",
                context("dev", "api", "123"),
                spec(
                    "dev",
                    new ACLs(
                        List.of(new ACLs.DefinitionACL("dict", List.of("123")), new ACLs.DefinitionACL("api", List.of("123", "456"))),
                        null
                    ),
                    null
                )
            ),
            arguments(
                "plugin acl ok",
                context("dev", "api", "123", plugin("foo")),
                spec(
                    "dev",
                    new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), List.of(new ACLs.PluginACL("foo", null))),
                    null
                )
            ),
            arguments(
                "plugin acl ok many",
                context("dev", "api", "123", plugin("foo")),
                spec(
                    "dev",
                    new ACLs(
                        List.of(new ACLs.DefinitionACL("api", List.of("123"))),
                        List.of(new ACLs.PluginACL("bar", null), new ACLs.PluginACL("foo", null))
                    ),
                    null
                )
            ),
            arguments(
                "plugin acl only",
                context("dev", "api", "123", plugin("foo")),
                spec("dev", new ACLs(null, List.of(new ACLs.PluginACL("foo", null))), null)
            ),
            arguments(
                "plugin acl only many",
                context("dev", "api", "123", plugin("foo")),
                spec(
                    "dev",
                    new ACLs(
                        List.of(), // setting an empty list for the sake of testing empty list
                        List.of(new ACLs.PluginACL("bar", null), new ACLs.PluginACL("foo", null))
                    ),
                    null
                )
            )
        );
    }

    public static Stream<Arguments> denials() {
        return Stream.of(
            arguments("no acl diff env", context("dev", "api", "123"), spec("test", null, null)),
            arguments("no acl diff key", context("dev", "api", "123", "pwd"), spec("dev", null, "pass")),
            arguments(
                "def acl ok wrong env",
                context("dev", "api", "123"),
                spec("test", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), null), null)
            ),
            arguments(
                "def acl ok wrong key",
                context("dev", "api", "123", "pwd"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), null), "pass")
            ),
            arguments(
                "def acl wrong id",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("456"))), null), null)
            ),
            arguments(
                "def acl wrong kind",
                context("dev", "api", "123"),
                spec("dev", new ACLs(List.of(new ACLs.DefinitionACL("dict", List.of("123"))), null), null)
            ),
            arguments(
                "plugin acl ko",
                context("dev", "api", "123", plugin("foo")),
                spec(
                    "dev",
                    new ACLs(List.of(new ACLs.DefinitionACL("api", List.of("123"))), List.of(new ACLs.PluginACL("bar", null))),
                    null
                )
            ),
            arguments(
                "plugin acl only ko",
                context("dev", "api", "123", plugin("bar")),
                spec("dev", new ACLs(null, List.of(new ACLs.PluginACL("foo", null))), null)
            ),
            arguments("no spec", context("dev", "api", "123", plugin("bar")), null)
        );
    }

    @MethodSource("grants")
    @ParameterizedTest(name = "{0}")
    void should_grant(String name, DiscoveryContext context, Spec spec) {
        assertThat(cut.grant(context, spec)).isTrue();
    }

    @MethodSource("denials")
    @ParameterizedTest(name = "{0}")
    void should_deny(String name, DiscoveryContext context, Spec spec) {
        assertThat(cut.grant(context, spec)).isFalse();
    }

    static DiscoveryContext context(String env, String kind, String id, String key, PayloadLocation... payloads) {
        return context(
            env,
            kind,
            id,
            new Ref(
                Ref.MainType.URI,
                new Ref.Expression("/mock/secret", false),
                Ref.SecondaryType.KEY,
                new Ref.Expression(key, false),
                "<< /mock/secret:%s >>".formatted(key)
            ),
            payloads
        );
    }

    static DiscoveryContext context(String env, String kind, String id, PayloadLocation... payloads) {
        return context(
            env,
            kind,
            id,
            new Ref(Ref.MainType.NAME, new Ref.Expression("secret", false), null, null, "<< secret >>"),
            payloads
        );
    }

    static DiscoveryContext context(String env, String kind, String id, Ref ref, PayloadLocation... payloads) {
        return new DiscoveryContext(null, env, ref, new DiscoveryLocation(new DiscoveryLocation.Definition(kind, id), payloads));
    }

    static Spec spec(String env, ACLs acls, String key) {
        return new Spec(null, "secret", null, key, null, key == null, false, null, acls, env);
    }

    static PayloadLocation plugin(String id) {
        return new PayloadLocation(PayloadLocation.PLUGIN_KIND, id);
    }
}
