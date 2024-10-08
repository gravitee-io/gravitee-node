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
package com.graviteesource.services.runtimesecrets.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.secrets.runtime.discovery.Ref;
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
class RefParserTest {

    public static Stream<Arguments> okRefs() {
        return Stream.of(
            arguments(
                "static uri short",
                "<< /vault/secrets/partners/apikeys:tesco >>",
                new Ref(
                    Ref.MainType.URI,
                    new Ref.Expression("/vault/secrets/partners/apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("tesco", false),
                    "<< /vault/secrets/partners/apikeys:tesco >>"
                )
            ),
            arguments(
                "static uri prefix",
                "<< uri /vault/secrets/partners/apikeys:tesco >>",
                new Ref(
                    Ref.MainType.URI,
                    new Ref.Expression("/vault/secrets/partners/apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("tesco", false),
                    "<< uri /vault/secrets/partners/apikeys:tesco >>"
                )
            ),
            arguments(
                "static uri long",
                "<< uri /vault/secrets/partners/apikeys key tesco >>",
                new Ref(
                    Ref.MainType.URI,
                    new Ref.Expression("/vault/secrets/partners/apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("tesco", false),
                    "<< uri /vault/secrets/partners/apikeys key tesco >>"
                )
            ),
            arguments(
                "static name short",
                "<< partners-apikeys >>",
                new Ref(Ref.MainType.NAME, new Ref.Expression("partners-apikeys", false), null, null, "<< partners-apikeys >>")
            ),
            arguments(
                "static name prefix",
                "<< name partners-apikeys >>",
                new Ref(Ref.MainType.NAME, new Ref.Expression("partners-apikeys", false), null, null, "<< name partners-apikeys >>")
            ),
            arguments(
                "dyn EL key",
                "<< uri /vault/secrets/partners/apikeys key {#context.attributes['secret-key']} >>",
                new Ref(
                    Ref.MainType.URI,
                    new Ref.Expression("/vault/secrets/partners/apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("#context.attributes['secret-key']", true),
                    "<< uri /vault/secrets/partners/apikeys key {#context.attributes['secret-key']} >>"
                )
            ),
            arguments(
                "dyn EL key short",
                "<< /vault/secrets/partners/apikeys:{#context.attributes['secret-key']} >>",
                new Ref(
                    Ref.MainType.URI,
                    new Ref.Expression("/vault/secrets/partners/apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("#context.attributes['secret-key']", true),
                    "<< /vault/secrets/partners/apikeys:{#context.attributes['secret-key']} >>"
                )
            ),
            arguments(
                "name and dyn EL key short",
                "<< partners-apikeys:{#context.attributes['secret-key']} >>",
                new Ref(
                    Ref.MainType.NAME,
                    new Ref.Expression("partners-apikeys", false),
                    Ref.SecondaryType.KEY,
                    new Ref.Expression("#context.attributes['secret-key']", true),
                    "<< partners-apikeys:{#context.attributes['secret-key']} >>"
                )
            ),
            arguments(
                "dynamic name",
                "<< name #context.attributes['secret-uri'] >>",
                new Ref(
                    Ref.MainType.NAME,
                    new Ref.Expression("#context.attributes['secret-uri']", true),
                    null,
                    null,
                    "<< name #context.attributes['secret-uri'] >>"
                )
            )
        );
    }

    @MethodSource("okRefs")
    @ParameterizedTest(name = "{0}")
    void should_parse(String name, String given, Ref expected) {
        assertThat(RefParser.parse(given)).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_parse_uri_and_key() {
        String expression = "/provider/secret:password";
        assertThat(RefParser.parseUriAndKey(expression))
            .usingRecursiveComparison()
            .isEqualTo(new RefParser.UriAndKey("/provider/secret", "password"));
    }
}
