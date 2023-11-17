package io.gravitee.node.vertx.server;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
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

    public AbstractVertxServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> trustStoreLoaderFactoryRegistry
    ) {
        this.vertx = vertx;
        this.keyStoreLoaderFactoryRegistry = keyStoreLoaderFactoryRegistry;
        this.trustStoreLoaderFactoryRegistry = trustStoreLoaderFactoryRegistry;
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
}
