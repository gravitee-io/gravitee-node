/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.api.license.model.LicensePack;
import io.gravitee.node.api.license.model.LicenseTier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author GraviteeSource Team
 */
class DefaultLicenseModelServiceTest {

    private final DefaultLicenseModelService cut = new DefaultLicenseModelService();

    private static final String[] STANDARD_FEATURES = {
        "apim-api-products",
        "apim-audit-trail",
        "apim-custom-roles",
        "apim-dcr-registration",
        "apim-debug-mode",
        "apim-openid-connect-sso",
        "apim-sharding-tags",
        "gravitee-risk-assessment",
        "apim-bridge-gateway",
        "apim-policy-xslt",
        "apim-policy-ws-security-authentication",
        "am-idp-gateway-handler-saml",
        "am-idp-azure-ad",
        "am-idp-cas",
        "am-idp-france-connect",
        "am-idp-http-flow",
        "am-idp-kerberos",
        "am-idp-ldap",
        "am-idp-salesforce",
        "am-idp-saml",
    };

    @ParameterizedTest(name = "tier ''{0}'' should contain expected packs")
    @MethodSource("tiers")
    void should_contain_expected_tier_packs(String tierName, String[] expectedPacks) {
        LicenseTier tier = getTier(tierName);

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks()).containsExactlyInAnyOrder(expectedPacks);
    }

    @ParameterizedTest(name = "pack ''{0}'' should contain expected features")
    @MethodSource("packs")
    void should_contain_expected_pack_features(String packName, String[] expectedFeatures) {
        LicensePack pack = getPack(packName);

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures()).containsExactlyInAnyOrder(expectedFeatures);
    }

    private static Stream<Arguments> tiers() {
        return Stream.of(
            arguments("planet", new String[] { "enterprise-features", "enterprise-legacy-upgrade", "enterprise-identity-provider" }),
            arguments(
                "galaxy",
                new String[] {
                    "enterprise-features",
                    "enterprise-legacy-upgrade",
                    "enterprise-identity-provider",
                    "observability",
                    "enterprise-policy",
                    "enterprise-alert-engine",
                }
            ),
            arguments(
                "universe",
                new String[] {
                    "enterprise-features",
                    "enterprise-legacy-upgrade",
                    "enterprise-identity-provider",
                    "observability",
                    "enterprise-policy",
                    "event-native",
                    "enterprise-mfa-factor",
                    "enterprise-secret-manager",
                    "enterprise-alert-engine",
                    "enterprise-authenticator",
                }
            )
        );
    }

    private static Stream<Arguments> packs() {
        return Stream.of(
            arguments("standard", STANDARD_FEATURES),
            // The enterprise pack extends standard through a YAML anchor and adds its own features.
            arguments(
                "enterprise",
                concat(
                    STANDARD_FEATURES,
                    new String[] {
                        "apim-reporter-datadog",
                        "apim-reporter-tcp",
                        "am-policy-account-linking",
                        "am-policy-mfa-challenge",
                        "apim-policy-assign-metrics",
                        "apim-policy-data-cache",
                        "apim-policy-data-logging-masking",
                        "apim-policy-geoip-filtering",
                        "apim-policy-graphql-ratelimit",
                        "apim-policy-interops-a-idp",
                        "apim-policy-interops-a-sp",
                        "apim-policy-interops-r-sp",
                        "apim-policy-oas-validation",
                        "apim-policy-transform-avro-json",
                        "apim-policy-transform-avro-protobuf",
                        "apim-policy-transform-protobuf-json",
                        "alert-engine",
                    }
                )
            ),
            arguments(
                "authorization-management",
                new String[] {
                    "gamma-authz-module",
                    "gamma-authz-reactor",
                    "gamma-authz-policy-authzen-pdp",
                    "gamma-authz-policy-pep",
                    "gamma-authz-service-pdp",
                }
            ),
            arguments(
                "identity-and-access-management",
                new String[] {
                    "am-mfa-call",
                    "am-mfa-fido2",
                    "am-mfa-http",
                    "am-mfa-otp-sender",
                    "am-mfa-recovery-code",
                    "am-mfa-sms",
                    "am-resource-http",
                    "am-resource-http-factor",
                    "am-resource-orange-contact-everyone",
                    "am-resource-sfr",
                    "am-resource-twilio",
                    "am-authenticator-cba",
                    "am-authenticator-magic-link",
                }
            ),
            arguments(
                "agent-management",
                new String[] {
                    "apim-ai-resource-model-token-classification",
                    "apim-a2a-proxy-reactor",
                    "apim-llm-proxy-reactor",
                    "apim-mcp-proxy-reactor",
                    "apim-mcp-tool-server",
                    "apim-ai-policy-semantic-cache",
                    "apim-policy-pii-filtering",
                    "apim-ai-resource-text-embedding",
                    "apim-ai-resource-vector-store-redis",
                    "gamma-aim-module",
                    "gamma-aim-endpoint-tools-http",
                    "gamma-aim-endpoint-tools-mcp",
                    "gamma-aim-entrypoint-mcp-studio",
                    "gamma-aim-policy-prompt-decorator",
                    "gamma-aim-policy-prompt-pattern-guard-rails",
                    "gamma-aim-policy-prompt-template",
                    "gamma-aim-policy-semantic-prompt-guard",
                    "gamma-aim-policy-semantic-response-guard",
                }
            ),
            arguments(
                "event-native-management",
                new String[] {
                    "apim-en-message-reactor",
                    "apim-en-entrypoint-agent-to-agent",
                    "apim-en-entrypoint-http-get",
                    "apim-en-entrypoint-http-post",
                    "apim-en-entrypoint-sse",
                    "apim-en-entrypoint-webhook",
                    "apim-en-entrypoint-websocket",
                    "apim-en-endpoint-agent-to-agent",
                    "apim-en-endpoint-asb",
                    "apim-en-endpoint-kafka",
                    "apim-en-endpoint-jms",
                    "apim-en-endpoint-mqtt5",
                    "apim-en-endpoint-rabbitmq",
                    "apim-en-endpoint-solace",
                    "apim-en-schema-registry-provider",
                    "gamma-en-module",
                }
            ),
            arguments(
                "event-streaming-management",
                new String[] {
                    "apim-cluster",
                    "apim-native-kafka-explorer",
                    "apim-native-kafka-reactor",
                    "apim-native-kafka-policy-acl",
                    "apim-native-kafka-policy-encryption",
                    "apim-native-kafka-policy-offloading",
                    "apim-native-kafka-policy-quota",
                    "apim-native-kafka-policy-topic-mapping",
                    "apim-native-kafka-policy-transform-key",
                    "apim-native-kafka-policy-virtual-topics",
                    "apim-native-kafka-policy-rules",
                    "apim-native-kafka-policy-namespace",
                    "apim-native-policy-ip-filtering",
                    "gamma-esm-module",
                }
            ),
            arguments("edge-management", new String[] { "gamma-edge-module", "gamma-edge-reactor" })
        );
    }

    private LicenseTier getTier(String tierName) {
        LicenseModel model = cut.getLicenseModel();
        return model.getTiers().get(tierName);
    }

    private LicensePack getPack(String packName) {
        LicenseModel model = cut.getLicenseModel();
        return model.getPacks().get(packName);
    }

    private static String[] concat(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
