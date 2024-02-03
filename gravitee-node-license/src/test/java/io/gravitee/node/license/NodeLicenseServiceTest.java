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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.NodeLicenseService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class NodeLicenseServiceTest {

    @Mock
    private Node node;

    @Mock
    private License license;

    @Mock
    private Feature tier;

    @Mock
    private Feature packs;

    @Mock
    private Feature features;

    private NodeLicenseService service;

    @BeforeEach
    void setUp() {
        openMocks(this);
        when(node.license()).thenReturn(license);

        when(license.feature("tier")).thenReturn(Optional.of(tier));
        when(license.feature("packs")).thenReturn(Optional.of(packs));
        when(license.feature("features")).thenReturn(Optional.of(features));

        service = new NodeLicenseServiceImpl(node, new LicenseModelServiceImpl());
    }

    @Test
    void shouldReturnTier() {
        when(tier.getString()).thenReturn("planet");
        when(license.feature("tier")).thenReturn(Optional.of(tier));
        service.refresh();
        assertThat(service.getTier()).isEqualTo("planet");
    }

    @Test
    void shouldReturnTierPacks() {
        when(tier.getString()).thenReturn("universe");
        service.refresh();
        assertThat(service.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-identity-provider",
                "enterprise-mfa-factor",
                "enterprise-legacy-upgrade",
                "observability",
                "event-native",
                "enterprise-policy",
                "enterprise-secret-manager",
                "enterprise-alert-engine"
            );
    }

    @Test
    void shouldReturnTierFeatures() {
        when(tier.getString()).thenReturn("universe");
        service.refresh();
        assertThat(service.getFeatures())
            .containsExactlyInAnyOrder(
                "apim-en-schema-registry-provider",
                "am-gateway-handler-saml-idp",
                "am-idp-saml",
                "apim-en-entrypoint-http-post",
                "http-flow-am-idp",
                "apim-policy-transform-avro-json",
                "apim-policy-transform-avro-protobuf",
                "apim-policy-transform-protobuf-json",
                "am-resource-twilio",
                "am-mfa-http",
                "apim-api-designer",
                "apim-custom-roles",
                "apim-en-endpoint-rabbitmq",
                "am-mfa-fido2",
                "apim-en-endpoint-solace",
                "apim-audit-trail",
                "am-mfa-call",
                "apim-debug-mode",
                "cas-am-idp",
                "am-resource-http-factor",
                "am-factor-http",
                "am-idp-azure-ad",
                "apim-reporter-datadog",
                "apim-dcr-registration",
                "am-factor-otp-sender",
                "apim-policy-ws-security-authentication",
                "am-mfa-resource-http-factor",
                "reporter-datadog",
                "apim-policy-geoip-filtering",
                "am-idp-cas",
                "am-mfa-recovery-code",
                "apim-en-entrypoint-http-get",
                "apim-connectors-advanced",
                "am-idp-ldap",
                "am-idp-france-connect",
                "risk-assessment",
                "apim-policy-xslt",
                "am-idp-salesforce",
                "am-factor-fido2",
                "am-mfa-otp-sender",
                "am-mfa-sms",
                "apim-en-endpoint-kafka",
                "gravitee-risk-assessment",
                "am-idp-http-flow",
                "apim-policy-assign-metrics",
                "policy-assign-metrics",
                "apim-openid-connect-sso",
                "apim-en-entrypoint-websocket",
                "apim-sharding-tags",
                "apim-policy-data-logging-masking",
                "apim-en-message-reactor",
                "apim-en-endpoint-mqtt5",
                "apim-en-entrypoint-webhook",
                "am-idp-kerberos",
                "apim-bridge-gateway",
                "apim-en-entrypoint-sse",
                "apim-reporter-tcp",
                "policy-data-logging-masking",
                "am-idp-gateway-handler-saml",
                "am-policy-mfa-challenge",
                "am-policy-account-linking",
                "am-resource-sfr",
                "gravitee-en-secretprovider-vault",
                "alert-engine"
            );
    }

    @Test
    void shouldAddFeatureToPack() {
        when(tier.getString()).thenReturn("planet");
        when(features.getString()).thenReturn("policy-assign-metrics");
        service.refresh();
        assertThat(service.getFeatures()).contains("policy-assign-metrics");
    }

    @Test
    void shouldSupportLegacyFeatures() {
        when(license.features()).thenReturn(Map.of("apim-api-designer", "included", "apim-bridge-gateway", "included"));
        service.refresh();
        assertThat(service.getFeatures()).containsExactlyInAnyOrder("apim-api-designer", "apim-bridge-gateway");
    }

    @Test
    void shouldHaveFeatureEnabled() {
        when(tier.getString()).thenReturn("universe");
        service.refresh();
        assertThat(service.isFeatureEnabled("apim-api-designer")).isTrue();
    }

    @Test
    void shouldHaveLegacyFeatureEnabled() {
        when(license.features()).thenReturn(Map.of("apim-api-designer", "included", "apim-bridge-gateway", "included"));
        service.refresh();
        assertThat(service.isFeatureEnabled("apim-api-designer")).isTrue();
    }
}
