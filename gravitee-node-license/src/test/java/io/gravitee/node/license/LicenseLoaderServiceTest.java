package io.gravitee.node.license;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.license.*;
import io.gravitee.node.license.management.NodeLicenseManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class LicenseLoaderServiceTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private LicenseFetcher licenseFetcher;

    @Mock
    private ManagementEndpointManager managementEndpointManager;

    private LicenseLoaderService cut;

    @BeforeEach
    public void init() throws Exception {
        cut = new LicenseLoaderService(licenseManager, licenseFetcher, managementEndpointManager);
    }

    @Test
    void should_start_without_loading_license_when_no_license_defined() throws Exception {
        cut.doStart();

        verify(licenseFetcher).fetch();
        verify(managementEndpointManager).register(any(NodeLicenseManagementEndpoint.class));
        verify(licenseManager).onLicenseExpires(any(Consumer.class));
        verifyNoMoreInteractions(licenseManager);
    }

    @Test
    void should_setup_license_when_start() throws Exception {
        final License license = mock(License.class);
        when(licenseFetcher.fetch()).thenReturn(license);

        cut.doStart();

        verify(licenseManager).registerPlatformLicense(license);
        verify(licenseManager).onLicenseExpires(any(Consumer.class));
        verify(licenseFetcher).startWatch(any(Consumer.class));
    }

    @Test
    void should_stop_node_when_loading_invalid_license() throws Exception {
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            final Runtime runtime = mock(Runtime.class);
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);

            when(licenseFetcher.fetch()).thenThrow(new InvalidLicenseException("Mock Invalid License"));
            cut.doStart();

            verify(licenseManager, never()).registerPlatformLicense(any());

            verify(runtime).exit(0);
        }
    }

    @Test
    void should_not_stop_node_when_loading_malformed_license() throws Exception {
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            final Runtime runtime = mock(Runtime.class);
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);

            when(licenseFetcher.fetch()).thenThrow(new MalformedLicenseException("Mock Malformed License"));
            cut.doStart();

            verify(licenseManager, never()).registerPlatformLicense(any());
        }
    }

    @Test
    void should_stop_watch_when_stopping_service() throws Exception {
        cut.doStop();

        verify(licenseFetcher).stopWatch();
    }

    @Test
    void should_stop_node_when_license_expires() throws Exception {
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            final Runtime runtime = mock(Runtime.class);
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtime);

            final License license = mock(License.class);
            final ArgumentCaptor<Consumer> consumerArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);

            when(licenseFetcher.fetch()).thenReturn(license);
            doNothing().when(licenseManager).onLicenseExpires(consumerArgumentCaptor.capture());

            cut.doStart();

            final Consumer<License> onExpired = consumerArgumentCaptor.getValue();
            onExpired.accept(license);

            verify(runtime).exit(0);
        }
    }

    @Test
    void should_setup_license_when_license_is_updated() throws Exception {
        final License license = mock(License.class);
        final License updatedLicense = mock(License.class);

        final ArgumentCaptor<Consumer> consumerArgumentCaptor = ArgumentCaptor.forClass(Consumer.class);

        when(licenseFetcher.fetch()).thenReturn(license);
        doNothing().when(licenseFetcher).startWatch(consumerArgumentCaptor.capture());

        cut.doStart();
        verify(licenseManager).registerPlatformLicense(license);

        final Consumer<License> onUpdated = consumerArgumentCaptor.getValue();
        onUpdated.accept(updatedLicense);

        verify(licenseManager).registerPlatformLicense(updatedLicense);
    }
}
