package io.gravitee.node.api.certificate;

import io.gravitee.common.util.KeyStoreUtils;
import java.security.KeyStore;
import java.util.Objects;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 *
 * Emited by {@link KeyStoreLoader} implementation. Comes in two flavor: {@link LoadEvent} and {@link UnloadEvent}
 */
public interface KeyStoreEvent {
    /**
     * Returns the id of the {@link KeyStoreLoader} that emitted this event
     * @return an id as a String
     */
    String loaderId();

    /**
     * Represent that a keystore was loaded by Gravitee.
     * No fields of that record can be null
     * @param loaderId which loader loaded the {@link KeyStore}
     * @param keyStore the loaded keystore
     * @param password the password that was configured or generated to protect the keystore
     */
    record LoadEvent(String loaderId, KeyStore keyStore, String password) implements KeyStoreEvent {
        public LoadEvent {
            Objects.requireNonNull(loaderId);
            Objects.requireNonNull(keyStore);
            Objects.requireNonNull(password);
        }

        /**
         * For convenience, returns the keystore password as {@link java.security.KeyStore.PasswordProtection}
         * @return the password as {@link java.security.KeyStore.PasswordProtection}
         */
        public KeyStore.PasswordProtection passwordAsProtection() {
            return new KeyStore.PasswordProtection(KeyStoreUtils.passwordToCharArray(password));
        }
    }

    /**
     * Represent a KeyStore that was unloaded. You'll note that the event is identified by its loader id, which assumes that all keystore loaded by this loaded will become unavailable to the end user
     * @param loaderId id of the loader that emitted that event, cannot be null
     */
    record UnloadEvent(String loaderId) implements KeyStoreEvent {
        public UnloadEvent {
            Objects.requireNonNull(loaderId);
        }
    }
}
