package io.gravitee.node.vertx.server;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.gravitee.node.vertx.cert.VertxKeyCertOptions;
import io.gravitee.node.vertx.cert.VertxTLSOptionsRegistry;
import io.gravitee.node.vertx.cert.VertxTrustOptions;
import io.vertx.rxjava3.core.Vertx;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractVertxServerFactory {

    protected final Vertx vertx;
    private final KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry;
    private final KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry;
    private final VertxTLSOptionsRegistry tlsOptionsRegistry;

    public AbstractVertxServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry,
        VertxTLSOptionsRegistry tlsOptionsRegistry
    ) {
        this.vertx = vertx;
        this.keyStoreLoaderFactoryRegistry = keyStoreLoaderFactoryRegistry;
        this.trustStoreLoaderFactoryRegistry = trustStoreLoaderFactoryRegistry;
        this.tlsOptionsRegistry = tlsOptionsRegistry;
    }

    protected KeyStoreLoaderManager createAndStartKeyManager(VertxServerOptions options) {
        String serverId = options.getId();
        KeyStoreLoader platformKeyStoreLoader = keyStoreLoaderFactoryRegistry.createLoader(options.getKeyStoreLoaderOptions(), serverId);
        KeyStoreLoaderManager keyStoreLoaderManager = new KeyStoreLoaderManager(serverId, platformKeyStoreLoader, options.isSni());
        try {
            tlsOptionsRegistry.registerOptions(serverId, new VertxKeyCertOptions(keyStoreLoaderManager.getKeyManager()));
            keyStoreLoaderManager.start();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start KeyStoreManager", e);
        }
        return keyStoreLoaderManager;
    }

    protected final TrustStoreLoaderManager createAnsStartCertificateManager(VertxServerOptions options) {
        String serverId = options.getId();
        KeyStoreLoader platformTrustStoreLoader = trustStoreLoaderFactoryRegistry.createLoader(
            options.getTrustStoreLoaderOptions(),
            serverId
        );
        TrustStoreLoaderManager trustStoreLoaderManager = new TrustStoreLoaderManager(serverId, platformTrustStoreLoader);
        tlsOptionsRegistry.registerOptions(serverId, new VertxTrustOptions(trustStoreLoaderManager.getCertificateManager()));
        try {
            trustStoreLoaderManager.start();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot start TrustStoreLoaderManager", e);
        }
        return trustStoreLoaderManager;
    }
}
