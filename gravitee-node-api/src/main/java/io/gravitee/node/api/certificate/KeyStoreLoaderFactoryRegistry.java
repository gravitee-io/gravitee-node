package io.gravitee.node.api.certificate;

import java.util.Set;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 *
 * Used to register all {@link KeyStoreLoaderFactory} is used essentally to platform truststore and keystore.
 */
public interface KeyStoreLoaderFactoryRegistry<O extends AbstractStoreLoaderOptions> {
    /**
     * Register a factory
     * @param keyStoreLoaderFactory the factory to register
     */
    void registerFactory(KeyStoreLoaderFactory<O> keyStoreLoaderFactory);

    /**
     *
     * @return all factories as a {@link Set}
     */
    Set<KeyStoreLoaderFactory<O>> getLoaderFactories();

    /**
     * Browse all registered {@link KeyStoreLoaderFactory} and select the first one where {@link KeyStoreLoaderFactory#canHandle(AbstractStoreLoaderOptions)} returns true.
     * It ensures that only one that can be selected by failing otherwise.
     * @param options to be tested against all factories
     * @return a {@link KeyStoreLoader} create by the one factory that matches the given options
     * @throws IllegalArgumentException if several factory matches options.
     */
    KeyStoreLoader createLoader(O options);
}
