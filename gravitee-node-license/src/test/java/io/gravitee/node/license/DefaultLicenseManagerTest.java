package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.node.license.license3j.License3J;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.core.api.PluginRegistry;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultLicenseManagerTest {

    private DefaultLicenseManager cut;

    @Mock
    private PluginRegistry pluginRegistry;

    @BeforeEach
    void init() {
        cut = new DefaultLicenseManager(pluginRegistry);
    }

    @Test
    void should_return_oss_license_when_no_platform_license_has_been_registered() {
        final License platformLicense = cut.getPlatformLicense();
        assertThat(platformLicense.getTier()).isEqualTo("oss");
    }

    @Test
    void should_register_platform_license() {
        final License license = mock(License.class);
        cut.registerPlatformLicense(license);

        final License platformLicense = cut.getPlatformLicense();
        assertThat(platformLicense).isSameAs(license);
    }

    @Test
    void should_return_null_license_when_no_organization_license_has_been_registered() {
        final License license = cut.getOrganizationLicense("orgId");
        assertThat(license).isNull();
    }

    @Test
    void should_return_platform_license_when_no_organization_license_has_been_registered() {
        final License license = cut.getOrganizationLicenseOrPlatform("orgId");
        assertThat(license).isSameAs(cut.getPlatformLicense());
    }

    @Test
    void should_register_organization_license() {
        final License license = mock(License.class);
        cut.registerOrganizationLicense("orgId", license);

        final License organizationLicense = cut.getOrganizationLicense("orgId");
        assertThat(organizationLicense).isSameAs(license);
    }

    @Test
    void should_validate_plugin_features_when_allowed_by_platform_license() {
        mockPluginRegistry();
        final License license = mock(License.class);

        // Allow the features on the license.
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(true);
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(true);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");
        final LicenseManager.Plugin mqtt5Plugin = new LicenseManager.Plugin("endpoint-connector", "mqtt5");

        cut.registerPlatformLicense(license);
        assertDoesNotThrow(() -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin, mqtt5Plugin)));
    }

    @Test
    void should_validate_plugin_features_when_empty_or_null() {
        assertDoesNotThrow(() -> cut.validatePluginFeatures("orgId", Collections.emptyList()));
        assertDoesNotThrow(() -> cut.validatePluginFeatures("orgId", null));
    }

    @Test
    void should_validate_plugin_features_when_allowed_by_organization_license() {
        mockPluginRegistry();
        final License license = mock(License.class);

        // Allow the features on the license.
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(true);
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(true);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");
        final LicenseManager.Plugin mqtt5Plugin = new LicenseManager.Plugin("endpoint-connector", "mqtt5");

        cut.registerOrganizationLicense("orgId", license);
        assertDoesNotThrow(() -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin, mqtt5Plugin)));
    }

    @Test
    void should_throw_forbidden_feature_when_license_is_expired() {
        mockPluginRegistry();

        final License3J license3J = mock(License3J.class);
        final License license = DefaultLicense
            .builder()
            .referenceType(License.REFERENCE_TYPE_PLATFORM)
            .referenceId(License.REFERENCE_ID_PLATFORM)
            .license3j(license3J)
            .build();

        when(license3J.isValid()).thenReturn(true);
        when(license3J.isExpired()).thenReturn(true);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");

        cut.registerPlatformLicense(license);
        assertThrows(ForbiddenFeatureException.class, () -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin)));
    }

    @Test
    void should_throw_forbidden_feature_when_license_is_not_valid() {
        mockPluginRegistry();

        final License3J license3J = mock(License3J.class);
        final License license = DefaultLicense
            .builder()
            .referenceType(License.REFERENCE_TYPE_PLATFORM)
            .referenceId(License.REFERENCE_ID_PLATFORM)
            .license3j(license3J)
            .build();

        when(license3J.isValid()).thenReturn(false);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");

        cut.registerPlatformLicense(license);
        assertThrows(ForbiddenFeatureException.class, () -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin)));
    }

    @Test
    void should_throw_forbidden_feature_when_validate_plugin_features_not_allowed_by_license() {
        mockPluginRegistry();

        final License license = mock(License.class);
        // Allow & disallow the features on the license.
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(false);
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(true);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");
        final LicenseManager.Plugin mqtt5Plugin = new LicenseManager.Plugin("endpoint-connector", "mqtt5");

        cut.registerOrganizationLicense("orgId", license);

        final ForbiddenFeatureException exception = assertThrows(
            ForbiddenFeatureException.class,
            () -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin, mqtt5Plugin))
        );

        assertThat(exception.getMessage())
            .isEqualTo("Plugin [kafka] cannot be loaded because the feature [apim-en-endpoint-kafka] is not allowed by the license.");
        assertThat(exception.getFeatures()).contains(new LicenseManager.ForbiddenFeature("apim-en-endpoint-kafka", "kafka"));
    }

    @Test
    void should_not_throw_error_on_unknown_plugin() throws Exception {
        mockPluginRegistry();

        final License license = mock(License.class);
        // Allow the features on the license.
        when(license.isFeatureEnabled("apim-en-endpoint-kafka")).thenReturn(true);
        when(license.isFeatureEnabled("apim-en-endpoint-mqtt5")).thenReturn(true);

        final LicenseManager.Plugin kafkaPlugin = new LicenseManager.Plugin("endpoint-connector", "kafka");
        final LicenseManager.Plugin mqtt5Plugin = new LicenseManager.Plugin("endpoint-connector", "mqtt5");
        final LicenseManager.Plugin unknownPlugin = new LicenseManager.Plugin("endpoint-connector", "other"); // This plugin is not in the plugin registry.

        cut.registerOrganizationLicense("orgId", license);

        assertDoesNotThrow(() -> cut.validatePluginFeatures("orgId", List.of(kafkaPlugin, mqtt5Plugin, unknownPlugin)));
    }

    @Test
    @SneakyThrows
    void should_notify_license_expired_when_license_is_expired() {
        final Calendar pastDate = Calendar.getInstance();
        pastDate.set(Calendar.DATE, -1);

        // License is expired for 10 days.
        final Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DATE, -10);

        final License license = mock(License.class);
        final Consumer<License> licenseExpiredListener = mock(Consumer.class);

        when(license.getExpirationDate()).thenReturn(expirationDate.getTime());

        cut.registerOrganizationLicense("orgId", license);
        cut.onLicenseExpires(licenseExpiredListener);

        try (MockedStatic<Calendar> calendarStatic = Mockito.mockStatic(Calendar.class)) {
            calendarStatic.when(Calendar::getInstance).thenReturn(pastDate);
            cut.start();

            verify(license, timeout(1000)).getExpirationDate();

            // Check the listener has been called.
            verify(licenseExpiredListener, timeout(5000)).accept(license);
        } finally {
            cut.doStop();
        }
    }

    @Test
    @SneakyThrows
    void should_notify_platform_license_expired_when_license_is_expired() {
        final Calendar pastDate = Calendar.getInstance();
        pastDate.set(Calendar.DATE, -1);

        // License is expired for 10 days.
        final Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DATE, -10);

        final License license = mock(License.class);
        final Consumer<License> licenseExpiredListener = mock(Consumer.class);

        when(license.getExpirationDate()).thenReturn(expirationDate.getTime());

        cut.registerPlatformLicense(license);
        cut.onLicenseExpires(licenseExpiredListener);

        try (MockedStatic<Calendar> calendarStatic = Mockito.mockStatic(Calendar.class)) {
            calendarStatic.when(Calendar::getInstance).thenReturn(pastDate);
            cut.start();

            verify(license, timeout(1000).times(1)).getExpirationDate();

            // Check the listener has been called.
            verify(licenseExpiredListener, timeout(5000)).accept(license);
        } finally {
            cut.doStop();
        }
    }

    @Test
    @SneakyThrows
    void should_just_warn_when_license_is_about_to_expired() {
        final Calendar pastDate = Calendar.getInstance();
        pastDate.set(Calendar.DATE, -1);

        // License will expire in 20 days (< 30 days).
        final Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DATE, 20);

        final License license = mock(License.class);
        final Consumer<License> licenseExpiredListener = mock(Consumer.class);

        when(license.getExpirationDate()).thenReturn(expirationDate.getTime());

        cut.registerOrganizationLicense("orgId", license);
        cut.onLicenseExpires(licenseExpiredListener);

        try (MockedStatic<Calendar> calendarStatic = Mockito.mockStatic(Calendar.class)) {
            calendarStatic.when(Calendar::getInstance).thenReturn(pastDate);
            cut.start();
            verify(license, timeout(1000).times(1)).getExpirationDate();

            // Check the listener has NOT been called.
            verify(licenseExpiredListener, times(0)).accept(license);
        } finally {
            cut.doStop();
        }
    }

    @Test
    @SneakyThrows
    void should_not_notify_when_license_is_not_expired_yet() {
        final Calendar pastDate = Calendar.getInstance();
        pastDate.set(Calendar.DATE, -1);

        // License will expire in 20 days (< 30 days).
        final Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.DATE, 360);

        final License license = mock(License.class);
        final Consumer<License> licenseExpiredListener = mock(Consumer.class);

        when(license.getExpirationDate()).thenReturn(expirationDate.getTime());

        cut.registerOrganizationLicense("orgId", license);
        cut.onLicenseExpires(licenseExpiredListener);

        try (MockedStatic<Calendar> calendarStatic = Mockito.mockStatic(Calendar.class)) {
            calendarStatic.when(Calendar::getInstance).thenReturn(pastDate);
            cut.start();
            verify(license, timeout(1000).times(1)).getExpirationDate();

            // Check the listener has NOT been called.
            verify(licenseExpiredListener, times(0)).accept(license);
        } finally {
            cut.doStop();
        }
    }

    private void mockPluginRegistry() {
        final io.gravitee.plugin.core.api.Plugin registryKafkaPlugin = mock(io.gravitee.plugin.core.api.Plugin.class);
        final PluginManifest kafkaPluginManifest = mock(PluginManifest.class);

        lenient().when(kafkaPluginManifest.feature()).thenReturn("apim-en-endpoint-kafka");
        lenient().when(registryKafkaPlugin.manifest()).thenReturn(kafkaPluginManifest);
        lenient().when(pluginRegistry.get("endpoint-connector", "kafka")).thenReturn(registryKafkaPlugin);

        final io.gravitee.plugin.core.api.Plugin registryMqtt5Plugin = mock(io.gravitee.plugin.core.api.Plugin.class);
        final PluginManifest mqTT5PluginManifest = mock(PluginManifest.class);

        lenient().when(mqTT5PluginManifest.feature()).thenReturn("apim-en-endpoint-mqtt5");
        lenient().when(registryMqtt5Plugin.manifest()).thenReturn(mqTT5PluginManifest);
        lenient().when(pluginRegistry.get("endpoint-connector", "mqtt5")).thenReturn(registryMqtt5Plugin);
    }
}
