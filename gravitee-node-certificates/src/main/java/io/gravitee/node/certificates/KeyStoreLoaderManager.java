package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.certificates.x509.RefreshableX509KeyManagerDelegator;
import javax.net.ssl.X509KeyManager;

/**
 * This class manages the unique {@link java.security.KeyStore} for TLS termination. It provides the {@link X509KeyManager} to be used by the server to do so.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreLoaderManager extends AbstractKeyStoreLoaderManager {

    /**
     * Construct the KeyStoreLoaderManager
     * @param target the target of this manager
     * @param platformKeyStoreLoader the platform keystore loader created from gravitee configuration
     * @param sniEnabled true is SNI should be considered when resolving certs
     * @param defaultAlias a fallback alias when no domain matches are found
     */
    public KeyStoreLoaderManager(String target, KeyStoreLoader platformKeyStoreLoader, boolean sniEnabled, String defaultAlias) {
        super(target, platformKeyStoreLoader, new RefreshableX509KeyManagerDelegator(target, sniEnabled));
        // Here we set the defaultAlias at the X509 manager level once and for all.
        // The alias is updated to reflect its internal value in AbstractKeyStoreLoaderManager.
        // Default alias is only used by RefreshableX509KeyManagerDelegator
        ((RefreshableX509KeyManagerDelegator) refreshableX509Manager).setDefaultAlias(scopeAlias(platformKeyStoreLoader, defaultAlias));
    }

    /**
     *
     * @return JDK {@link javax.net.ssl.KeyManager}
     */
    public X509KeyManager getKeyManager() {
        return (X509KeyManager) refreshableX509Manager;
    }
}
