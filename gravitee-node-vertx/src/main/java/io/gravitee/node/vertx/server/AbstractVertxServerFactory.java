package io.gravitee.node.vertx.server;

import io.gravitee.node.api.certificate.*;
import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.vertx.rxjava3.core.Vertx;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractVertxServerFactory {

    protected final Vertx vertx;
    private final KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry;
    private final KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry;
    private final CRLLoaderFactoryRegistry crlLoaderFactoryRegistry;

    public AbstractVertxServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry,
        CRLLoaderFactoryRegistry crlLoaderFactoryRegistry
    ) {
        this.vertx = vertx;
        this.keyStoreLoaderFactoryRegistry = keyStoreLoaderFactoryRegistry;
        this.trustStoreLoaderFactoryRegistry = trustStoreLoaderFactoryRegistry;
        this.crlLoaderFactoryRegistry = crlLoaderFactoryRegistry;
    }

    protected KeyStoreLoaderManager createKeyManager(VertxServerOptions options) {
        KeyStoreLoader platformKeyStoreLoader = keyStoreLoaderFactoryRegistry.createLoader(options.getKeyStoreLoaderOptions());
        return new KeyStoreLoaderManager(
            "server{id: %s}".formatted(options.getId()),
            platformKeyStoreLoader,
            options.isSni(),
            options.getKeyStoreLoaderOptions().getDefaultAlias()
        );
    }

    protected final TrustStoreLoaderManager createCertificateManager(VertxServerOptions options) {
        KeyStoreLoader platformTrustStoreLoader = trustStoreLoaderFactoryRegistry.createLoader(options.getTrustStoreLoaderOptions());
        return new TrustStoreLoaderManager("server{id: %s}".formatted(options.getId()), platformTrustStoreLoader);
    }

    protected CRLLoaderManager createCRLManager(TrustStoreLoaderManager trustStoreManager, CRLLoaderOptions crlLoaderOptions) {
        CRLLoader crlLoader = crlLoaderFactoryRegistry.createLoader(crlLoaderOptions);
        CRLLoaderManager crlLoaderManager = new CRLLoaderManager((CRLRefreshable) trustStoreManager.getCertificateManager());
        crlLoaderManager.registerCrlLoader(crlLoader);
        return crlLoaderManager;
    }
}
