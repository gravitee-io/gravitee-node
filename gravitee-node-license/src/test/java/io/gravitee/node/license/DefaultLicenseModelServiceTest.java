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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.license.model.LicenseAttribute;
import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.api.license.model.LicensePack;
import io.gravitee.node.api.license.model.LicenseTier;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @Test
    void should_default_gamma_universe_portals_max_to_10() {
        assertThat(getTier("gamma-universe").getAttributes()).containsExactly(entry("apim-portals-max", 10));
    }

    @Test
    void should_declare_portals_max_as_an_integer_limit() {
        LicenseAttribute attribute = cut.getLicenseModel().getAttributes().get("apim-portals-max");

        assertThat(attribute.getType()).isEqualTo(LicenseAttribute.Type.INTEGER);
        assertThat(attribute.isLimit()).isTrue();
    }

    @Test
    void should_load_tier_attribute_matching_its_declaration() {
        LicenseModel model = parse(
            """
            attributes:
                apim-portals-max:
                    type: integer
                    limit: true
                support-level:
                    type: string
            tiers:
                custom:
                    packs: []
                    attributes:
                        apim-portals-max: 10
                        support-level: gold
            """
        );

        assertThat(model.getTiers().get("custom").getAttributes())
            .containsEntry("apim-portals-max", 10)
            .containsEntry("support-level", "gold");
        assertThat(model.getAttributes().get("support-level").isLimit()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidModels")
    void should_reject_invalid_attributes(String name, String yaml, String message) {
        assertThatThrownBy(() -> parse(yaml))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(message);
    }

    private static Stream<Arguments> invalidModels() {
        return Stream.of(
            arguments(
                "undeclared tier attribute",
                """
                attributes:
                    apim-portals-max:
                        type: integer
                tiers:
                    custom:
                        packs: []
                        attributes:
                            apim-portal-max: 10
                """,
                "tier 'custom' sets the undeclared attribute 'apim-portal-max'"
            ),
            arguments(
                "tier attribute of the wrong type",
                """
                attributes:
                    apim-portals-max:
                        type: integer
                tiers:
                    custom:
                        packs: []
                        attributes:
                            apim-portals-max: ten
                """,
                "tier 'custom' sets the attribute 'apim-portals-max' to [ten], which is not of type integer"
            ),
            arguments(
                "attribute declared without a type",
                """
                attributes:
                    apim-portals-max:
                        limit: true
                tiers: {}
                """,
                "attribute 'apim-portals-max' declares no type"
            ),
            arguments(
                "limit declared on a string attribute",
                """
                attributes:
                    support-level:
                        type: string
                        limit: true
                tiers: {}
                """,
                "attribute 'support-level' is a limit, so it must be of type integer, not string"
            )
        );
    }

    private static LicenseModel parse(String yaml) {
        return DefaultLicenseModelService.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest(name = "tier ''{0}'' should have no attributes")
    @ValueSource(strings = { "planet", "galaxy", "universe", "gamma-planet", "gamma-galaxy" })
    void should_have_no_attributes(String tierName) {
        assertThat(getTier(tierName).getAttributes()).isEmpty();
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
                        "apim-policy-generate-wssecurity",
                        "apim-policy-geoip-filtering",
                        "apim-policy-graphql-ratelimit",
                        "apim-policy-interops-a-idp",
                        "apim-policy-interops-a-sp",
                        "apim-policy-interops-r-sp",
                        "apim-policy-oas-validation",
                        "apim-policy-token-exchange",
                        "apim-policy-transform-avro-json",
                        "apim-policy-transform-avro-protobuf",
                        "apim-policy-transform-protobuf-json",
                        "alert-engine",
                        "am-certificate-aws",
                        "am-certificate-aws-cloudhsm",
                        "am-extgrant-xaa",
                        "am-policy-authzen",
                        "gravitee-en-secretprovider-aws",
                        "gravitee-en-secretprovider-azure-keyvault",
                        "gravitee-en-secretprovider-vault",
                        "gravitee-en-secrets",
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
                    "gamma-aim-endpoint-tools-arazzo",
                    "gamma-aim-entrypoint-mcp-studio",
                    "gamma-aim-policy-prompt-decorator",
                    "gamma-aim-policy-prompt-pattern-guard-rails",
                    "gamma-aim-policy-prompt-template",
                    "gamma-aim-policy-semantic-prompt-guard",
                    "gamma-aim-policy-semantic-response-guard",
                    "apim-policy-token-ratelimit",
                    "apim-ai-policy-token-compression",
                    "apim-ai-policy-routing",
                    "apim-policy-cost-ratelimit",
                    "apim-ai-resource-vector-store-aws-s3",
                    "apim-guardians",
                    "apim-guardian-policy-rule",
                    "apim-guardian-policy-pii-redaction",
                    "apim-guardian-policy-decision",
                    "apim-guardian-policy-http-evaluator",
                    "apim-guardian-resource-decision-model",
                    "apim-guardian-resource-http-evaluator",
                    "gamma-aim-guardrails",
                    "gamma-aim-guardrails-policy-llm-rule",
                    "gamma-aim-guardrails-policy-llm-decision-binary",
                    "gamma-aim-guardrails-policy-llm-decision-scoring",
                    "gamma-aim-guardrails-policy-llm-pii-redaction",
                    "gamma-aim-guardrails-policy-llm-http-evaluator",
                    "gamma-aim-guardrails-resource-decision-model-remote",
                    "gamma-aim-guardrails-resource-token-classification-remote",
                    "apim-hitl",
                    "gamma-aim-discovery-foundry",
                    "gamma-aim-discovery-gemini",
                    "gamma-aim-discovery-bedrock",
                }
            ),
            arguments(
                "agent-mesh",
                new String[] {
                    "apim-mcp-proxy-reactor",
                    "apim-mcp-tool-server",
                    "apim-llm-proxy-reactor",
                    "apim-a2a-proxy-reactor",
                    "apim-ai-resource-text-embedding",
                    "apim-ai-resource-model-token-classification",
                    "apim-ai-resource-vector-store-redis",
                    "apim-ai-policy-semantic-cache",
                    "apim-policy-token-ratelimit",
                    "apim-policy-pii-filtering",
                    "apim-ai-resource-vector-store-aws-s3",
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
                    "apim-native-kafka-policy-message-filtering",
                    "apim-native-kafka-policy-data-contract",
                    "apim-native-policy-ip-filtering",
                    "gamma-esm-module",
                }
            ),
            arguments("edge-management", new String[] { "gamma-edge-module", "gamma-edge-reactor" }),
            arguments("gravitee-cloud", new String[] { "cache-cloud-redis" })
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
