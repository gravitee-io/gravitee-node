package io.gravitee.node.api.certificate;

public interface CRLLoaderFactory {
    boolean canHandle(CRLLoaderOptions options);

    CRLLoader create(CRLLoaderOptions options);
}
