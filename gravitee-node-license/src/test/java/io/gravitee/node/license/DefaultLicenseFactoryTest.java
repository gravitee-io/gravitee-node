package io.gravitee.node.license;

import static io.gravitee.node.api.license.License.REFERENCE_ID_PLATFORM;
import static io.gravitee.node.api.license.License.REFERENCE_TYPE_ORGANIZATION;
import static io.gravitee.node.api.license.License.REFERENCE_TYPE_PLATFORM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.MalformedLicenseException;
import io.gravitee.node.license.license3j.License3J;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax0.license3j.Feature;
import javax0.license3j.crypto.LicenseKeyPair;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultLicenseFactoryTest {

    private static final String INVALID_LICENSE =
        "Ic5OXgAAABcAAAACAAAABwAAAARjb21wYW55dGVzdAAAACEAAAACAAAABQAAABBlbWFpbHRlc3RAZ3Jhdml0ZWUuaW8AAAAaAAAACwAAAApleHBpcnlEYXRlAAABi5YJi1w" +
        "AAAAhAAAADAAAAAlsaWNlbnNlSWSJNcURfozT6Dz/mg0RFEjmAAAAnAAAAAEAAAAQAAAAgGxpY2Vuc2VTaWduYXR1cmWD2AVRDn0G07Yn4fXIx/vz4f8gu3RPNWt" +
        "JlsrGyRpLp+0du2lt7sFea/RJNNTqyNAWrtABljvf5dKcNxa4JIANKINX3t+508k6SejaUs0kOqfcL7Sztv2IbqJddIqCxPRsZInL9Htw7beMBJ9XYAGCgaHIrAN" +
        "VgTPojI4gvmfD2QAAACIAAAACAAAADwAAAAdzaWduYXR1cmVEaWdlc3RTSEEtMjU2AAAAGAAAAAIAAAAEAAAACHRpZXJ1bml2ZXJzZQ==";
    private static final String NULL_LICENSE = null;
    private static final byte[] NULL_BYTES_ARRAY = null;
    protected static final String ORG_ID = "orgId";

    private static LicenseKeyPair keyPair;
    private final DefaultLicenseFactory cut = new DefaultLicenseFactory(new DefaultLicenseModelService());

    MockedStatic<License3J> mockedStaticLicense3j;

    @BeforeAll
    static void init() throws NoSuchAlgorithmException {
        keyPair = LicenseKeyPair.Create.from("RSA", 1024);
    }

    @BeforeEach
    public void setUp() {
        mockedStaticLicense3j = mockStatic(License3J.class);
        mockedStaticLicense3j.when(License3J::publicKey).thenReturn(keyPair.getPublic());
    }

    @AfterEach
    public void tearDown() {
        mockedStaticLicense3j.close();
    }

    @Test
    void should_return_license_from_base64() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License("universe", null, null, null)
        );

        assertUniverseLicense(license);
    }

    @Test
    void should_return_license_from_bytes() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBytesLicense("universe", null, null, null)
        );
        assertUniverseLicense(license);
    }

    @Test
    void should_return_license_with_pack() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License(null, List.of("event-native"), null, null)
        );

        assertThat(license.getTier()).isNull();
        assertThat(license.getPacks()).containsExactly("event-native");
        assertThat(license.getFeatures())
            .containsExactlyInAnyOrder(
                "apim-en-schema-registry-provider",
                "apim-en-entrypoint-webhook",
                "apim-en-endpoint-rabbitmq",
                "apim-en-entrypoint-websocket",
                "apim-en-entrypoint-sse",
                "apim-en-endpoint-solace",
                "apim-en-entrypoint-http-get",
                "apim-connectors-advanced",
                "apim-en-message-reactor",
                "apim-en-endpoint-mqtt5",
                "apim-en-entrypoint-http-post",
                "apim-en-endpoint-kafka",
                "apim-en-endpoint-asb",
                "apim-en-entrypoint-agent-to-agent",
                "apim-en-endpoint-agent-to-agent"
            );
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(REFERENCE_ID_PLATFORM);
    }

    @Test
    void should_return_license_with_enterprise_authorization_engine_pack() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License(null, List.of("enterprise-authorization-engine"), null, null)
        );

        assertThat(license.getTier()).isNull();
        assertThat(license.getPacks()).containsExactly("enterprise-authorization-engine");
        assertThat(license.getFeatures())
            .containsExactlyInAnyOrder("am-authorizationengine-openfga", "am-authorization-gateway-handler-authzen");
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(REFERENCE_ID_PLATFORM);
    }

    @Test
    void should_return_license_with_enterprise_authenticator_pack() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License(null, List.of("enterprise-authenticator"), null, null)
        );

        assertThat(license.getTier()).isNull();
        assertThat(license.getPacks()).containsExactly("enterprise-authenticator");
        assertThat(license.getFeatures()).containsExactlyInAnyOrder("am-authenticator-cba");
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(REFERENCE_ID_PLATFORM);
    }

    @Test
    void should_return_license_with_features() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License(null, null, List.of("apim-en-message-reactor", "apim-en-endpoint-mqtt5"), null)
        );

        assertThat(license.getTier()).isNull();
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).containsExactlyInAnyOrder("apim-en-message-reactor", "apim-en-endpoint-mqtt5");
    }

    @Test
    void should_return_license_with_legacy_features() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateLegacyLicense(List.of("apim-api-designer", "apim-bridge-gateway")).serialized()
        );

        assertThat(license.getTier()).isNull();
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).containsExactlyInAnyOrder("apim-api-designer", "apim-bridge-gateway");
    }

    @Test
    void should_return_license_with_unknown_tier() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License("unknown", List.of("enterprise-legacy-upgrade"), null, null)
        );

        assertThat(license.getTier()).isEqualTo("unknown");
        assertThat(license.getPacks()).containsExactly("enterprise-legacy-upgrade");
        assertThat(license.getFeatures()).containsExactlyInAnyOrder("apim-policy-xslt", "apim-policy-ws-security-authentication");
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(REFERENCE_ID_PLATFORM);
    }

    @Test
    void should_return_license_with_unknown_pack() throws InvalidLicenseException, MalformedLicenseException {
        final License license = cut.create(
            REFERENCE_TYPE_PLATFORM,
            REFERENCE_ID_PLATFORM,
            generateBase64License("planet", List.of("unknown"), null, null)
        );

        assertThat(license.getTier()).isEqualTo("planet");
        assertThat(license.getPacks())
            .containsExactlyInAnyOrder("enterprise-features", "enterprise-legacy-upgrade", "enterprise-identity-provider", "unknown");
        assertThat(license.getFeatures())
            .containsExactlyInAnyOrder(
                "apim-api-designer",
                "apim-dcr-registration",
                "apim-custom-roles",
                "apim-audit-trail",
                "apim-sharding-tags",
                "apim-openid-connect-sso",
                "apim-debug-mode",
                "gravitee-risk-assessment",
                "risk-assessment",
                "apim-bridge-gateway",
                "apim-policy-xslt",
                "apim-policy-ws-security-authentication",
                "am-idp-salesforce",
                "am-idp-saml",
                "am-idp-ldap",
                "am-idp-kerberos",
                "am-idp-azure-ad",
                "am-idp-gateway-handler-saml",
                "am-gateway-handler-saml-idp",
                "am-idp-http-flow",
                "http-flow-am-idp",
                "am-idp-france-connect",
                "am-idp-cas",
                "cas-am-idp"
            );
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(REFERENCE_ID_PLATFORM);
    }

    @Test
    void should_throw_invalid_license_when_platform_license_is_invalid() {
        assertThrows(InvalidLicenseException.class, () -> cut.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, INVALID_LICENSE));
    }

    @Test
    void should_throw_invalid_license_when_platform_license_is_expired() {
        assertThrows(
            InvalidLicenseException.class,
            () ->
                cut.create(
                    REFERENCE_TYPE_PLATFORM,
                    REFERENCE_ID_PLATFORM,
                    generateBytesLicense("universe", null, null, new Date(System.currentTimeMillis() - 3600000))
                )
        );
    }

    @Test
    void should_throw_malformed_license_when_platform_license_is_unreadable() {
        assertThrows(
            MalformedLicenseException.class,
            () -> cut.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, "unreadable license".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_org_license_is_invalid() {
        final License license = cut.create(REFERENCE_TYPE_ORGANIZATION, ORG_ID, INVALID_LICENSE);

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_ORGANIZATION);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_expired_license_when_org_license_is_expired() {
        final License license = cut.create(
            REFERENCE_TYPE_ORGANIZATION,
            ORG_ID,
            generateBytesLicense("universe", null, null, new Date(System.currentTimeMillis() - 3600000))
        );

        assertThat(license.getTier()).isEqualTo("universe");
        assertThat(license.getPacks()).isNotEmpty();
        assertThat(license.getFeatures()).isNotEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_ORGANIZATION);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_org_license_is_unreadable() {
        final License license = cut.create(REFERENCE_TYPE_ORGANIZATION, ORG_ID, "unreadable license".getBytes(StandardCharsets.UTF_8));

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_ORGANIZATION);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_org_license_is_null() {
        final License license = cut.create(REFERENCE_TYPE_ORGANIZATION, ORG_ID, NULL_LICENSE);

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_ORGANIZATION);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_platform_license_is_null() {
        final License license = cut.create(REFERENCE_TYPE_PLATFORM, ORG_ID, NULL_LICENSE);

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_org_bytes_array_is_null() {
        final License license = cut.create(REFERENCE_TYPE_ORGANIZATION, ORG_ID, NULL_BYTES_ARRAY);

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_ORGANIZATION);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    @Test
    @SneakyThrows
    void should_return_oss_license_when_platform_bytes_array_is_null() {
        final License license = cut.create(REFERENCE_TYPE_PLATFORM, ORG_ID, NULL_BYTES_ARRAY);

        assertThat(license.getTier()).isEqualTo(OSSLicense.TIER);
        assertThat(license.getPacks()).isEmpty();
        assertThat(license.getFeatures()).isEmpty();
        assertThat(license.getReferenceType()).isEqualTo(REFERENCE_TYPE_PLATFORM);
        assertThat(license.getReferenceId()).isEqualTo(ORG_ID);
    }

    private void assertUniverseLicense(License license) {
        assertThat(license.getTier()).isEqualTo("universe");
        assertThat(license.getPacks())
            .containsExactlyInAnyOrder(
                "enterprise-features",
                "enterprise-identity-provider",
                "enterprise-alert-engine",
                "enterprise-mfa-factor",
                "enterprise-secret-manager",
                "enterprise-legacy-upgrade",
                "observability",
                "enterprise-policy",
                "event-native",
                "enterprise-authenticator"
            );

        final String[] features = {
            "apim-en-schema-registry-provider",
            "alert-engine",
            "am-gateway-handler-saml-idp",
            "am-idp-saml",
            "apim-en-entrypoint-http-post",
            "http-flow-am-idp",
            "apim-policy-transform-avro-json",
            "apim-policy-transform-protobuf-json",
            "apim-policy-transform-avro-protobuf",
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
            "apim-en-endpoint-asb",
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
            "am-resource-orange-contact-everyone",
            "am-resource-http",
            "gravitee-en-secretprovider-vault",
            "gravitee-en-secretprovider-aws",
            "gravitee-en-secrets",
            "am-certificate-aws",
            "am-certificate-aws-cloudhsm",
            "apim-policy-graphql-ratelimit",
            "apim-policy-interops-a-idp",
            "apim-policy-interops-r-idp",
            "apim-policy-interops-a-sp",
            "apim-policy-interops-r-sp",
            "apim-policy-oas-validation",
            "apim-policy-data-cache",
            "apim-en-entrypoint-agent-to-agent",
            "apim-en-endpoint-agent-to-agent",
            "am-authenticator-cba",
        };
        assertThat(license.getFeatures()).containsExactlyInAnyOrder(features);

        // Check the license is valid.
        assertDoesNotThrow(license::verify);

        // Check feature are enabled.
        for (String feature : features) {
            assertThat(license.isFeatureEnabled(feature)).isTrue();
        }

        // Check expiration date is ok.
        assertThat(license.getExpirationDate()).isAfter(Instant.now());

        // Assert other attributes and raw.
        assertThat(license.getAttributes())
            .containsEntry("company", "test")
            .containsEntry("email", "test@gravitee.io")
            .containsEntry("anInteger", 123)
            .containsEntry("aDate", new Date(0));

        assertThat(license.getRawAttributes())
            .containsEntry("company", "test")
            .containsEntry("email", "test@gravitee.io")
            .containsEntry("anInteger", "123")
            .containsEntry("aDate", "1970-01-01 00:00:00.000");
    }

    @SneakyThrows
    private byte[] generateBytesLicense(String tier, List<String> packs, List<String> features, Date expiry) {
        javax0.license3j.License license = generateLicense(tier, packs, features, expiry);

        return license.serialized();
    }

    @SneakyThrows
    private String generateBase64License(String tier, List<String> packs, List<String> features, Date expiry) {
        javax0.license3j.License license = generateLicense(tier, packs, features, expiry);

        return Base64.getEncoder().encodeToString(license.serialized());
    }

    @SneakyThrows
    private javax0.license3j.License generateLicense(String tier, List<String> packs, List<String> features, Date expiry) {
        javax0.license3j.License license = new javax0.license3j.License();
        license.setLicenseId();
        license.setExpiry(Objects.requireNonNullElseGet(expiry, () -> new Date(System.currentTimeMillis() + 360000)));
        license.add(Feature.Create.stringFeature("company", "test"));
        license.add(Feature.Create.stringFeature("email", "test@gravitee.io"));
        license.add(Feature.Create.intFeature("anInteger", 123));
        license.add(Feature.Create.dateFeature("aDate", new Date(0)));

        if (tier != null) {
            license.add(Feature.Create.stringFeature("tier", tier));
        }

        if (packs != null) {
            license.add(Feature.Create.stringFeature("packs", String.join(",", packs)));
        }

        if (features != null) {
            license.add(Feature.Create.stringFeature("features", String.join(",", features)));
        }

        license.sign(keyPair.getPair().getPrivate(), "SHA-256");

        return license;
    }

    @SneakyThrows
    private javax0.license3j.License generateLegacyLicense(List<String> features) {
        javax0.license3j.License license = new javax0.license3j.License();
        license.setLicenseId();
        license.setExpiry(new Date(System.currentTimeMillis() + 360000));
        license.add(Feature.Create.stringFeature("company", "test"));
        license.add(Feature.Create.stringFeature("email", "test@gravitee.io"));

        features.forEach(feature -> license.add(Feature.Create.stringFeature(feature, "included")));

        license.sign(keyPair.getPair().getPrivate(), "SHA-256");

        return license;
    }
}
