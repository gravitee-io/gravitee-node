package io.gravitee.node.api.certificate;

import java.security.KeyStore;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 * This interface represent X.509 certificate or key manager that can be refreshed with new keystore.
 */
public interface RefreshableX509Manager {
    /**
     * Call when a keystore needs to be refreshed. It assumes that keystore passed is complete and the logic to add/remove aliases are handle beforehand.
     * @param keyStore the keystore to refresh
     * @param password the keystore password.
     */
    void refresh(KeyStore keyStore, char[] password);
}
