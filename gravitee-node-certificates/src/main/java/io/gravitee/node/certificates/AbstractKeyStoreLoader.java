package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 *
 * Base implementation for all {@link KeyStoreLoader} it deals with holding option and provider a unified way to tigger the event handler.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractKeyStoreLoader<O extends AbstractStoreLoaderOptions> implements KeyStoreLoader {

    private final String id;
    private final String password;
    private Consumer<KeyStoreEvent> handler;

    protected final O options;

    /**
     * Assigns an id, get password from options or generate one if not provided.
     * @param options the options to be processed
     */
    protected AbstractKeyStoreLoader(O options) {
        this.id = UUID.randomUUID().toString();
        this.options = options;
        this.password = Optional.ofNullable(options.getPassword()).orElse(UUID.randomUUID().toString());
    }

    /**
     * @return the keystore password.
     * Any party willing to get the keystore password should use this method at the options might contain one.
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Default implementation as a simple setter
     * @param handler {@link KeyStoreEvent} handler, must not be null
     */
    @Override
    public final void setEventHandler(@Nonnull Consumer<KeyStoreEvent> handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public String id() {
        return id;
    }

    /**
     * Trigger the event handler. To be called when a keystore is loaded or unloaded.
     * @param event the event to process
     */
    public final void onEvent(KeyStoreEvent event) {
        this.handler.accept(event);
    }
}
