package io.gravitee.node.api.certificate;

import java.util.Set;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface KeyStoreLoaderFactoryRegistry<O extends AbstractStoreLoaderOptions> {
    void registerFactory(KeyStoreLoaderFactory<O> keyStoreLoaderFactory);

    Set<KeyStoreLoaderFactory<O>> getLoaderFactories();

    KeyStoreLoader createLoader(O options, String serverId);
}
