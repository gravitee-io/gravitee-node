package io.gravitee.node.api.certificate;

import java.util.Set;

public interface CRLLoaderFactoryRegistry {
    void registerFactory(CRLLoaderFactory crlLoaderFactory);

    Set<CRLLoaderFactory> getLoaderFactories();

    CRLLoader createLoader(CRLLoaderOptions options);
}
