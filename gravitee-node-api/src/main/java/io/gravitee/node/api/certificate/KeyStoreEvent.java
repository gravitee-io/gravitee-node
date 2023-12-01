package io.gravitee.node.api.certificate;

import io.gravitee.common.util.KeyStoreUtils;
import java.security.KeyStore;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record KeyStoreEvent(EventType type, String loaderId, KeyStore keyStore, String password, String defaultAlias) {
    private KeyStoreEvent(String loaderId) {
        this(EventType.UNLOAD, loaderId, null, null, null);
    }
    private KeyStoreEvent(String loaderId, KeyStore keyStore, String password, String defaultAlias) {
        this(EventType.LOAD, loaderId, keyStore, password, defaultAlias);
    }
    public KeyStoreEvent {
        Objects.requireNonNull(type);
        Objects.requireNonNull(loaderId);
        if (keyStore != null) {
            Objects.requireNonNull(password);
        }
    }

    public static KeyStoreEvent loadEvent(String loaderId, KeyStore keyStore, String password, String defaultAlias) {
        return new KeyStoreEvent(loaderId, keyStore, password, defaultAlias);
    }

    public static KeyStoreEvent unloadEvent(String loaderId) {
        return new KeyStoreEvent(loaderId);
    }

    public KeyStore.PasswordProtection passwordAsProtection() {
        return new KeyStore.PasswordProtection(KeyStoreUtils.passwordToCharArray(password));
    }

    public enum EventType {
        LOAD,
        UNLOAD,
    }
}
