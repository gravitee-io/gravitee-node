package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.AbstractStoreLoaderOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractKeyStoreLoader<O extends AbstractStoreLoaderOptions> implements KeyStoreLoader {

    private final String id = UUID.randomUUID().toString();
    private final String password;
    private Consumer<KeyStoreEvent> handler;

    protected final O options;

    protected AbstractKeyStoreLoader(O options) {
        this.options = options;
        this.password = Optional.ofNullable(options.getPassword()).orElse(UUID.randomUUID().toString());
    }

    public final String getPassword() {
        return password;
    }

    @Override
    public void setEventHandler(Consumer<KeyStoreEvent> handler) {
        this.handler = handler;
    }

    @Override
    public String id() {
        return id;
    }

    public void onEvent(KeyStoreEvent event) {
        this.handler.accept(event);
    }
}
