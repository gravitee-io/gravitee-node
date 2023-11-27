package io.gravitee.node.api.certificate;

import io.gravitee.common.util.KeyStoreUtils;
import java.security.KeyStore;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record KeyStoreEvent(EventType type, String loaderId, KeyStore keyStore, String password, String defaultAlias) {
    public KeyStoreEvent {
        Objects.requireNonNull(type);
        Objects.requireNonNull(loaderId);
        if (keyStore != null) {
            Objects.requireNonNull(password);
        }
    }

    public KeyStore.PasswordProtection passwordAsProtection() {
        return new KeyStore.PasswordProtection(KeyStoreUtils.passwordToCharArray(password));
    }

    public enum EventType {
        LOAD,
        UNLOAD,
    }
}
