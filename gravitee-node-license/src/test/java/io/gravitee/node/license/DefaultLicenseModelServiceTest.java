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

import io.gravitee.node.api.license.model.LicenseModel;
import io.gravitee.node.api.license.model.LicensePack;
import io.gravitee.node.api.license.model.LicenseTier;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DefaultLicenseModelServiceTest {

    private final DefaultLicenseModelService cut = new DefaultLicenseModelService();

    @Test
    void should_contain_expected_planet_tier_packs() {
        LicenseTier tier = getTier("planet");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder("enterprise-features", "enterprise-legacy-upgrade", "enterprise-identity-provider");
    }

    @Test
    void should_contain_expected_galaxy_tier_packs() {
        LicenseTier tier = getTier("galaxy");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-legacy-upgrade",
                "enterprise-identity-provider",
                "observability",
                "enterprise-policy",
                "enterprise-alert-engine"
            );
    }

    @Test
    void should_contain_expected_universe_tier_packs() {
        LicenseTier tier = getTier("universe");

        assertThat(tier).isNotNull();
        assertThat(tier.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-legacy-upgrade",
                "enterprise-identity-provider",
                "observability",
                "enterprise-policy",
                "event-native",
                "enterprise-mfa-factor",
                "enterprise-secret-manager",
                "enterprise-alert-engine",
                "enterprise-authenticator"
            );
    }

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

    @Test
    void should_contain_expected_standard_pack_features() {
        LicensePack pack = getPack("standard");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures()).containsExactlyInAnyOrder(STANDARD_FEATURES);
    }

    @Test
    void should_contain_expected_enterprise_pack_features() {
        LicensePack pack = getPack("enterprise");

        String[] enterpriseFeatures = {
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
        };

        assertThat(pack).isNotNull();
        // The enterprise pack extends standard through a YAML anchor and adds its own features.
        assertThat(pack.getFeatures()).containsExactlyInAnyOrder(concat(STANDARD_FEATURES, enterpriseFeatures));
    }

    @Test
    void should_contain_expected_authorization_management_pack_features() {
        LicensePack pack = getPack("authorization-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures())
            .containsExactlyInAnyOrder(
                "gamma-authz-module",
                "gamma-authz-reactor",
                "gamma-authz-policy-authzen-pdp",
                "gamma-authz-policy-pep",
                "gamma-authz-service-pdp"
            );
    }

    @Test
    void should_contain_expected_identity_and_access_management_pack_features() {
        LicensePack pack = getPack("identity-and-access-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures())
            .containsExactlyInAnyOrder(
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
                "am-authenticator-magic-link"
            );
    }

    @Test
    void should_contain_expected_agent_management_pack_features() {
        LicensePack pack = getPack("agent-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures())
            .containsExactlyInAnyOrder(
                "apim-ai-resource-token-classification",
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
                "gamma-aim-policy-semantic-response-guard"
            );
    }

    @Test
    void should_contain_expected_event_native_management_pack_features() {
        LicensePack pack = getPack("event-native-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures())
            .containsExactlyInAnyOrder(
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
                "gamma-en-module"
            );
    }

    @Test
    void should_contain_expected_event_streaming_management_pack_features() {
        LicensePack pack = getPack("event-streaming-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures())
            .containsExactlyInAnyOrder(
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
                "apim-native-policy-ip-filtering",
                "gamma-esm-module"
            );
    }

    @Test
    void should_contain_expected_edge_management_pack_features() {
        LicensePack pack = getPack("edge-management");

        assertThat(pack).isNotNull();
        assertThat(pack.getFeatures()).containsExactlyInAnyOrder("gamma-edge-module", "gamma-edge-reactor");
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
