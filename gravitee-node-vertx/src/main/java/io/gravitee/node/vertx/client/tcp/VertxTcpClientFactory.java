package io.gravitee.node.vertx.client.tcp;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetClient;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class VertxTcpClientFactory {

    protected static final String TCP_SSL_OPENSSL_CONFIGURATION = "tcp.ssl.openssl";

    @NonNull
    private final Vertx vertx;

    @NonNull
    private final Configuration nodeConfiguration;

    private final VertxTcpTarget tcpTarget;
    private VertxTcpClientOptions tcpOptions;
    private SslOptions sslOptions;
    private VertxTcpProxyOptions proxyOptions;

    public NetClient createTcpClient() {
        if (tcpOptions == null) {
            tcpOptions = new VertxTcpClientOptions();
        }

        return vertx.createNetClient(createNetClientOptions());
    }

    private NetClientOptions createNetClientOptions() {
        var clientOptions = new NetClientOptions().setMetricsName("tcp-client");
        clientOptions
            .setConnectTimeout(tcpOptions.getConnectTimeout())
            .setReconnectAttempts(tcpOptions.getReconnectAttempts())
            .setReconnectInterval(tcpOptions.getReconnectInterval())
            .setIdleTimeout(tcpOptions.getIdleTimeout())
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS)
            .setReadIdleTimeout(tcpOptions.getIdleTimeout())
            .setWriteIdleTimeout(tcpOptions.getIdleTimeout());

        configureSsl(clientOptions);
        configureTcpProxy(clientOptions);

        return clientOptions;
    }

    private void configureTcpProxy(NetClientOptions clientOptions) {
        if (proxyOptions != null && proxyOptions.isEnabled()) {
            if (proxyOptions.isUseSystemProxy()) {
                setSystemProxy(clientOptions);
            } else {
                ProxyOptions newProxyOptions = new ProxyOptions();
                newProxyOptions.setHost(proxyOptions.getHost());
                newProxyOptions.setPort(proxyOptions.getPort());
                newProxyOptions.setUsername(proxyOptions.getUsername());
                newProxyOptions.setPassword(proxyOptions.getPassword());
                newProxyOptions.setType(ProxyType.valueOf(this.proxyOptions.getType().name()));
                clientOptions.setProxyOptions(newProxyOptions);
            }
        }
    }

    private void setSystemProxy(NetClientOptions clientOptions) {
        try {
            clientOptions.setProxyOptions(VertxProxyOptionsUtils.buildProxyOptions(nodeConfiguration));
        } catch (Exception e) {
            log.warn(
                "TcpClient (target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}",
                tcpTarget,
                e.getMessage()
            );
            log.warn("Ignoring system proxy");
        }
    }

    private void configureSsl(final NetClientOptions clientOptions) {
        clientOptions.setSsl(tcpTarget.isSecured());
        if (sslOptions != null) {
            sslOptions.setOpenSsl(Boolean.TRUE.equals(nodeConfiguration.getProperty(TCP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)));
            try {
                configureSslClientOption(clientOptions, sslOptions);
            } catch (KeyStore.KeyStoreCertOptionsException | TrustStore.TrustOptionsException e) {
                throw new IllegalArgumentException(e.getMessage() + " for " + tcpTarget);
            }
        }
    }

    public static void configureSslClientOption(NetClientOptions clientOptions, SslOptions sslOptions)
        throws KeyStore.KeyStoreCertOptionsException, TrustStore.TrustOptionsException {
        if (sslOptions.isOpenSsl()) {
            clientOptions.setSslEngineOptions(new OpenSSLEngineOptions());
        }

        String hostnameVerificationAlgorithm = sslOptions.getHostnameVerificationAlgorithm();
        if ("NONE".equals(hostnameVerificationAlgorithm)) {
            clientOptions.setHostnameVerificationAlgorithm("");
        } else {
            clientOptions.setHostnameVerificationAlgorithm(hostnameVerificationAlgorithm);
        }

        clientOptions.setTrustAll(sslOptions.isTrustAll());

        if (!sslOptions.isTrustAll()) {
            // Client truststore configuration (trust server certificate).
            sslOptions.trustStore().flatMap(TrustStore::trustOptions).ifPresent(clientOptions::setTrustOptions);
        }

        // Client keystore configuration (client certificate for mtls).
        sslOptions.keyStore().flatMap(KeyStore::keyCertOptions).ifPresent(clientOptions::setKeyCertOptions);
    }
}
