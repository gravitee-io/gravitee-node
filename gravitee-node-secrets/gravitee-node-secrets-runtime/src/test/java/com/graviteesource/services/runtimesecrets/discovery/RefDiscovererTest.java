package com.graviteesource.services.runtimesecrets.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
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
class RefDiscovererTest {

    public static Stream<Arguments> payloads() {
        return Stream.of(
            arguments(
                "one smaller",
                """
                        {
                            "username": "admin",
                            "password": "<< uri /vault/secrets/redis:password >>"
                        }
                        """,
                List.of("<< uri /vault/secrets/redis:password >>"),
                List.of("secret"),
                List.of("\"password\": \"secret\"")
            ),
            arguments(
                "one bigger",
                """
                        {
                            "username": "admin",
                            "password": "<< uri /vault/secrets/redis:password >>"
                        }
                        """,
                List.of("<< uri /vault/secrets/redis:password >>"),
                List.of(
                    "{#secret.fromGrant('262fc907-ef40-47e0-b076-001ca79282da', 'f49ea02d-cd44-44dd-aa6d-04bb2a57a6ac-/vault/secrets/redis', 'password')}"
                ),
                List.of(
                    "\"password\": \"{#secret.fromGrant('262fc907-ef40-47e0-b076-001ca79282da', 'f49ea02d-cd44-44dd-aa6d-04bb2a57a6ac-/vault/secrets/redis', 'password')}\""
                )
            ),
            arguments(
                "mixed bigger",
                """
                        {
                            "username": "admin",
                            "password": "<< uri /vault/secrets/redis:password >>"
                            "token": "<< name redis-token >>"
                        }
                        """,
                List.of("<< uri /vault/secrets/redis:password >>", "<< name redis-token >>"),
                List.of(
                    "{#secret.fromGrant('ce06fe04-3cd4-4513-bf15-5aa446ab2c27', 'f49ea02d-cd44-44dd-aa6d-04bb2a57a6ac-/vault/secrets/redis', 'password')}",
                    "ABCDEFGH"
                ),
                List.of(
                    "\"password\": \"{#secret.fromGrant('ce06fe04-3cd4-4513-bf15-5aa446ab2c27', 'f49ea02d-cd44-44dd-aa6d-04bb2a57a6ac-/vault/secrets/redis', 'password')}\"",
                    "\"token\": \"ABCDEFGH\""
                )
            )
        );
    }

    @MethodSource("payloads")
    @ParameterizedTest(name = "{0}")
    void should_replace_refs_in_payloads(
        String name,
        String payload,
        List<String> refs,
        List<String> replacements,
        List<String> testString
    ) {
        PayloadRefParser disco = new PayloadRefParser(payload);
        disco.runDiscovery();
        assertThat(disco.getRawRefs().stream().map(PayloadRefParser.RawSecretRef::ref)).containsAnyElementsOf(refs);
        String result = disco.replaceRefs(replacements);
        assertThat(result).contains(testString);
    }
}
