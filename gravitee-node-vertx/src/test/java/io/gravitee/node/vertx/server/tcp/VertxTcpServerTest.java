package io.gravitee.node.vertx.server.tcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.cert.VertxKeyCertOptions;
import io.gravitee.node.vertx.cert.VertxTrustOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetServer;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class VertxTcpServerTest {

    public static final String ID = "my-tcp";

    @Mock
    private Vertx vertx;

    @Mock
    private VertxTcpServerOptions options;

    @Mock
    private NetServer delegate;

    @Mock
    private NetServerOptions netServerOptions;

    private VertxTcpServer cut;

    @Mock
    private KeyStoreLoaderManager keyStoreLoaderManager;

    @Mock
    private TrustStoreLoaderManager trustStoreLoaderManager;

    @Mock
    private CRLLoaderManager crlLoaderManager;

    @BeforeEach
    void init() {
        lenient()
            .when(options.createNetServerOptions(any(VertxKeyCertOptions.class), any(VertxTrustOptions.class)))
            .thenReturn(netServerOptions);
        lenient().when(vertx.createNetServer(netServerOptions)).thenReturn(delegate);
        lenient().when(keyStoreLoaderManager.getKeyManager()).thenReturn(mock(X509KeyManager.class));
        lenient().when(trustStoreLoaderManager.getCertificateManager()).thenReturn(mock(X509TrustManager.class));
        cut = new VertxTcpServer(vertx, options, keyStoreLoaderManager, trustStoreLoaderManager, crlLoaderManager);
    }

    @Test
    void should_return_net_type() {
        assertThat(cut.type()).isEqualTo(VertxTcpServer.KIND);
    }

    @Test
    void should_instantiate_vertx_net_server() {
        final NetServer netServer = cut.newInstance();

        assertThat(netServer).isNotNull();
        assertThat(cut.instances()).containsExactly(netServer);
        assertThat(cut.options()).isSameAs(options);
    }

    @Test
    void should_instantiate_multiple_vertx_net_servers() {
        when(options.createNetServerOptions(any(VertxKeyCertOptions.class), any(VertxTrustOptions.class))).thenReturn(netServerOptions);
        when(vertx.createNetServer(netServerOptions)).thenReturn(delegate);

        final NetServer netServer1 = cut.newInstance();
        final NetServer netServer2 = cut.newInstance();
        final NetServer netServer3 = cut.newInstance();

        assertThat(cut.instances()).containsExactly(netServer1, netServer2, netServer3);
    }
}
