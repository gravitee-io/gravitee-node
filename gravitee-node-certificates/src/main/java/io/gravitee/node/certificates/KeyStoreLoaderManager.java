package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.certificates.x509.RefreshableX509KeyManagerDelegator;
import javax.net.ssl.X509KeyManager;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreLoaderManager extends AbstractKeyStoreLoaderManager {

    public KeyStoreLoaderManager(String serverId, KeyStoreLoader platformKeyStoreLoader, boolean sniEnabled) {
        super(serverId, platformKeyStoreLoader, new RefreshableX509KeyManagerDelegator(serverId, sniEnabled));
    }

    public X509KeyManager getKeyManager() {
        return (X509KeyManager) refreshableX509Manager;
    }
}
