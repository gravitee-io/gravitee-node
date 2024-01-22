package io.gravitee.node.license;

import static io.gravitee.node.api.license.License.REFERENCE_ID_PLATFORM;
import static io.gravitee.node.api.license.License.REFERENCE_TYPE_PLATFORM;
import static io.gravitee.node.license.LicenseLoaderService.GRAVITEE_LICENSE_KEY;
import static io.gravitee.node.license.LicenseLoaderService.GRAVITEE_LICENSE_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.license.*;
import io.gravitee.node.license.management.NodeLicenseManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LicenseLoaderServiceTest {

    @Mock
    private Configuration configuration;

    @Mock
    private LicenseFactory licenseFactory;

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private ManagementEndpointManager managementEndpointManager;

    private LicenseLoaderService cut;

    @BeforeEach
    public void init() throws Exception {
        cut = new LicenseLoaderService(configuration, licenseFactory, licenseManager, managementEndpointManager);
    }

    @Test
    void should_start_without_loading_license_when_no_license_defined() throws Exception {
        cut.doStart();

        verify(managementEndpointManager).register(any(NodeLicenseManagementEndpoint.class));
        verify(licenseManager).onLicenseExpires(any(Consumer.class));
        verifyNoMoreInteractions(licenseManager);
    }

    @Test
    void should_start_and_load_license_from_environment() throws Exception {
        final License license = mock(License.class);
        final byte[] licenseBytes = "base64LicenseKey".getBytes(StandardCharsets.UTF_8);

        when(configuration.getProperty(GRAVITEE_LICENSE_KEY)).thenReturn(Base64.getEncoder().encodeToString(licenseBytes));
        when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes)).thenReturn(license);
        cut.doStart();

        verify(licenseManager).registerPlatformLicense(license);
    }

    @Test
    void should_start_and_load_license_from_file() throws Exception {
        try {
            final License license = mock(License.class);
            final byte[] licenseBytes = "base64LicenseKey".getBytes(StandardCharsets.UTF_8);
            final Path licence = Files.createTempFile("license", ".key");

            Files.write(licence, licenseBytes);
            licence.toFile().deleteOnExit();

            System.setProperty(GRAVITEE_LICENSE_PROPERTY, licence.toFile().getAbsolutePath());
            when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes)).thenReturn(license);
            cut.doStart();

            verify(licenseManager).registerPlatformLicense(license);
        } finally {
            System.clearProperty(GRAVITEE_LICENSE_PROPERTY);
        }
    }

    @Test
    void should_stop_when_loading_invalid_license() throws Exception {
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            final Runtime runtime = mock(Runtime.class);
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);

            final License license = mock(License.class);
            final byte[] licenseBytes = "invalidBase64LicenseKey".getBytes(StandardCharsets.UTF_8);

            when(configuration.getProperty(GRAVITEE_LICENSE_KEY)).thenReturn(Base64.getEncoder().encodeToString(licenseBytes));
            when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes))
                .thenThrow(new InvalidLicenseException("Mock Invalid License"));
            cut.doStart();
            verify(licenseManager, never()).registerPlatformLicense(license);

            verify(runtime).exit(0);
        }
    }

    @Test
    void should_not_stop_node_when_loading_malformed_license() throws Exception {
        final License license = mock(License.class);
        final byte[] licenseBytes = "malformedBase64LicenseKey".getBytes(StandardCharsets.UTF_8);
        when(configuration.getProperty(GRAVITEE_LICENSE_KEY)).thenReturn(Base64.getEncoder().encodeToString(licenseBytes));

        when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes))
            .thenThrow(new MalformedLicenseException("Mock Malformed License"));
        cut.doStart();
        verify(licenseManager, never()).registerPlatformLicense(license);
    }
}
