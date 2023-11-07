package io.gravitee.node.license.management;

import static io.gravitee.node.license.DefaultLicenseManager.OSS_LICENSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;
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
class NodeLicenseManagementEndpointTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private RoutingContext routingContext;

    private NodeLicenseManagementEndpoint cut;

    @BeforeEach
    void init() {
        cut = new NodeLicenseManagementEndpoint(licenseManager);
    }

    @Test
    void should_return_path() {
        assertThat(cut.path()).isEqualTo("/license");
    }

    @Test
    void should_return_method() {
        assertThat(cut.method()).isEqualTo(HttpMethod.GET);
    }

    @Test
    void should_return_platform_license_info() {
        final HttpServerResponse httpServerResponse = mock(HttpServerResponse.class);
        final License license = mock(License.class);

        when(routingContext.response()).thenReturn(httpServerResponse);
        when(licenseManager.getPlatformLicense()).thenReturn(license);

        Map<String, String> rawAttributes = new HashMap<>();
        rawAttributes.put("expiryDate", "2024-12-31 23:59:59.999");
        rawAttributes.put("tier", "test");
        rawAttributes.put("packs", "test-pack1,test-pack2");
        rawAttributes.put("features", null);
        rawAttributes.put("legacy-feature", "included");

        when(license.getRawAttributes()).thenReturn(rawAttributes);

        cut.handle(routingContext);

        verify(httpServerResponse).setStatusCode(200);
        verify(httpServerResponse).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(httpServerResponse).setChunked(true);
        verify(httpServerResponse)
            .end(
                """
                                {
                                  "expiryDate" : "2024-12-31 23:59:59.999",
                                  "features" : null,
                                  "tier" : "test",
                                  "legacy-feature" : "included",
                                  "packs" : "test-pack1,test-pack2"
                                }"""
            );
    }

    @Test
    void should_return_oss_license_info() {
        final HttpServerResponse httpServerResponse = mock(HttpServerResponse.class);

        when(routingContext.response()).thenReturn(httpServerResponse);
        when(licenseManager.getPlatformLicense()).thenReturn(OSS_LICENSE);

        cut.handle(routingContext);

        verify(httpServerResponse).setStatusCode(200);
        verify(httpServerResponse).putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        verify(httpServerResponse).setChunked(true);
        verify(httpServerResponse).end("""
                {
                  "tier" : "oss"
                }""");
    }
}
