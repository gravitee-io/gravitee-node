package io.gravitee.node.license;

import static io.gravitee.node.api.license.License.REFERENCE_ID_PLATFORM;
import static io.gravitee.node.api.license.License.REFERENCE_TYPE_PLATFORM;
import static io.gravitee.node.license.DefaultLicenseFetcher.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultLicenseFetcherTest {

    @Mock
    private Configuration configuration;

    @Mock
    private LicenseFactory licenseFactory;

    private DefaultLicenseFetcher cut;

    private String systemHomeProperty;
    private String systemLicenseProperty;

    @BeforeEach
    public void init() throws Exception {
        cut = new DefaultLicenseFetcher(configuration, licenseFactory);

        // Backup existing system properties that could be overriden during tests.
        systemHomeProperty = System.getProperty(GRAVITEE_HOME_PROPERTY);
        systemLicenseProperty = System.getProperty(GRAVITEE_LICENSE_PROPERTY);

        System.clearProperty(GRAVITEE_HOME_PROPERTY);
        System.clearProperty(GRAVITEE_LICENSE_PROPERTY);
    }

    @AfterEach
    public void cleanup() {
        // Restore existing system properties.
        Optional.ofNullable(systemHomeProperty).ifPresent(prop -> System.setProperty(GRAVITEE_HOME_PROPERTY, prop));
        Optional.ofNullable(systemLicenseProperty).ifPresent(prop -> System.setProperty(GRAVITEE_LICENSE_PROPERTY, prop));
    }

    @Test
    void should_fetch_license_from_environment() throws Exception {
        final License license = mock(License.class);
        final byte[] licenseBytes = "base64LicenseKey".getBytes(StandardCharsets.UTF_8);

        when(configuration.getProperty(GRAVITEE_LICENSE_KEY)).thenReturn(Base64.getEncoder().encodeToString(licenseBytes));
        when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes)).thenReturn(license);
        final License loaded = cut.fetch();

        assertThat(loaded).isEqualTo(license);
    }

    @Test
    void should_fetch_license_from_file() throws Exception {
        try {
            final License license = mock(License.class);
            final byte[] licenseBytes = "base64LicenseKey".getBytes(StandardCharsets.UTF_8);
            final Path licence = Files.createTempFile("license", ".key");

            Files.write(licence, licenseBytes);
            licence.toFile().deleteOnExit();

            System.setProperty(GRAVITEE_LICENSE_PROPERTY, licence.toFile().getAbsolutePath());
            when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes)).thenReturn(license);
            final License loaded = cut.fetch();

            assertThat(loaded).isEqualTo(license);
        } finally {
            System.clearProperty(GRAVITEE_LICENSE_PROPERTY);
        }
    }

    @Test
    void should_fetch_license_from_file_located_in_gravitee_home() throws Exception {
        final License license = mock(License.class);
        final byte[] licenseBytes = "base64LicenseKey".getBytes(StandardCharsets.UTF_8);
        final Path graviteeHome = Files.createTempDirectory("temp");

        // Create /license folder into gravitee home.
        final File licenseFolder = new File(graviteeHome.toFile().getAbsolutePath() + File.separator + "license");
        assertThat(licenseFolder.mkdir()).isTrue();

        // Write the fake license.key file.
        Files.write(new File(licenseFolder.toPath() + File.separator + "license.key").toPath(), licenseBytes);

        graviteeHome.toFile().deleteOnExit();
        System.setProperty(GRAVITEE_HOME_PROPERTY, graviteeHome.toFile().getAbsolutePath());

        when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes)).thenReturn(license);
        final License loaded = cut.fetch();

        assertThat(loaded).isEqualTo(license);
    }

    @Test
    void should_fetch_null_license_when_no_file_located_in_gravitee_home() throws Exception {
        final Path graviteeHome = Files.createTempDirectory("temp");
        graviteeHome.toFile().deleteOnExit();
        System.setProperty(GRAVITEE_HOME_PROPERTY, graviteeHome.toFile().getAbsolutePath());

        final License loaded = cut.fetch();

        assertThat(loaded).isNull();
    }

    @Test
    void should_fetch_null_license_when_unknown_file() throws Exception {
        System.setProperty(GRAVITEE_LICENSE_PROPERTY, "unknown.key");
        final License loaded = cut.fetch();

        assertThat(loaded).isNull();
    }

    @Test
    void should_throw_invalid_license_when_invalid_license() throws Exception {
        final byte[] licenseBytes = "invalidBase64LicenseKey".getBytes(StandardCharsets.UTF_8);

        when(configuration.getProperty(GRAVITEE_LICENSE_KEY)).thenReturn(Base64.getEncoder().encodeToString(licenseBytes));
        when(licenseFactory.create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, licenseBytes))
            .thenThrow(new InvalidLicenseException("Mock Invalid License"));

        assertThrows(InvalidLicenseException.class, () -> cut.fetch());
    }

    @Test
    void should_watch_and_create_license_when_file_is_updated() throws Exception {
        final Timer licenseUpdaterTimer = new Timer("license-updater", true);

        try {
            final Path licence = Files.createTempFile("license", ".key");

            licence.toFile().deleteOnExit();

            System.setProperty(GRAVITEE_LICENSE_PROPERTY, licence.toFile().getAbsolutePath());

            cut.startWatch(updatedLicense -> {});

            licenseUpdaterTimer.schedule(
                new TimerTask() {
                    @Override
                    @SneakyThrows
                    public void run() {
                        Files.writeString(licence, "base64LicenseKey");
                    }
                },
                100,
                30000
            );

            verify(licenseFactory, timeout(30000))
                .create(REFERENCE_TYPE_PLATFORM, REFERENCE_ID_PLATFORM, "base64LicenseKey".getBytes(StandardCharsets.UTF_8));
        } finally {
            licenseUpdaterTimer.cancel();
            cut.stopWatch();
        }
    }
}
