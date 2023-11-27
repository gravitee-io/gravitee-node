package io.gravitee.node.api.certificate;

import java.security.KeyStore;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface RefreshableX509Manager {
    void refresh(KeyStore keyStore, char[] password, String defaultAlias);
}
