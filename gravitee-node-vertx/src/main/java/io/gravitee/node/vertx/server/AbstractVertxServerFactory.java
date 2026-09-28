package io.gravitee.node.vertx.server;

import io.gravitee.node.api.certificate.*;
import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.vertx.core.http.ClientAuth;
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
        return new TrustStoreLoaderManager(
            "server{id: %s}".formatted(options.getId()),
            platformTrustStoreLoader,
            new TrustStoreLoaderManager.Options(options.isSendClientCertificateAuthorities(), failOnUntrustedClientCertificate(options))
        );
    }

    /**
     * A client certificate that no trust anchor validates breaks the handshake, except on a listener that only
     * <em>requests</em> one and has no configured trust store.
     * <p>
     * On such a listener the whole trust store is made of entries registered at runtime, the per-subscription
     * certificates of mTLS plans. Validating against them turns an unrelated subscription into the listener's trust
     * policy: before the first one is registered every certificate is accepted, after it only subscribed ones are,
     * so the very same call succeeds or fails depending on what is deployed elsewhere. Since presenting a
     * certificate is optional there, an unknown one is left to the application, which matches it against the
     * subscriptions of the plan being called and answers 401 rather than a TLS alert.
     * <p>
     * A configured trust store is an explicit trust policy, so it keeps being enforced, and
     * {@code clientAuth: required} stays strict in every case.
     */
    private static boolean failOnUntrustedClientCertificate(VertxServerOptions options) {
        boolean certificateOnlyRequested = ClientAuth.REQUEST.name().equalsIgnoreCase(options.getClientAuth());
        TrustStoreLoaderOptions trustStoreOptions = options.getTrustStoreLoaderOptions();
        boolean trustStoreConfigured = trustStoreOptions != null && trustStoreOptions.namesASource();
        return !certificateOnlyRequested || trustStoreConfigured;
    }

    protected CRLLoaderManager createCRLManager(TrustStoreLoaderManager trustStoreManager, CRLLoaderOptions crlLoaderOptions) {
        CRLLoader crlLoader = crlLoaderFactoryRegistry.createLoader(crlLoaderOptions);
        CRLLoaderManager crlLoaderManager = new CRLLoaderManager((CRLRefreshable) trustStoreManager.getCertificateManager());
        crlLoaderManager.registerCrlLoader(crlLoader);
        return crlLoaderManager;
    }
}
