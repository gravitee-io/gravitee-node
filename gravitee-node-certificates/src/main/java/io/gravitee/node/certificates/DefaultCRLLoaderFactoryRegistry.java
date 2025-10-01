package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLLoaderFactory;
import io.gravitee.node.api.certificate.CRLLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.security.cert.CRL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultCRLLoaderFactoryRegistry implements CRLLoaderFactoryRegistry {

    private static final CRLLoader NO_OP_CRL_LOADER = new NoOpCRLLoader();
    private final Set<CRLLoaderFactory> factories = new HashSet<>();

    @Override
    public void registerFactory(CRLLoaderFactory crlLoaderFactory) {
        log.debug("Registering CRL loader factory: {}", crlLoaderFactory.getClass().getSimpleName());
        factories.add(crlLoaderFactory);
    }

    @Override
    public Set<CRLLoaderFactory> getLoaderFactories() {
        return Set.copyOf(factories);
    }

    @Override
    public CRLLoader createLoader(CRLLoaderOptions options) {
        if (options == null || !options.isConfigured()) {
            log.debug("CRL options not configured, returning no-op CRL loader");
            return NO_OP_CRL_LOADER;
        }

        List<CRLLoaderFactory> matchingFactories = getLoaderFactories().stream().filter(factory -> factory.canHandle(options)).toList();

        if (matchingFactories.isEmpty()) {
            throw new IllegalArgumentException("No CRL loader factory found for path: %s".formatted(options.getPath()));
        }

        if (matchingFactories.size() > 1) {
            throw new IllegalArgumentException("Multiple CRL loader factories found for path: %s".formatted(options.getPath()));
        }

        CRLLoaderFactory factory = matchingFactories.get(0);
        log.info("Creating CRL loader using factory: {}", factory.getClass().getSimpleName());
        return factory.create(options);
    }

    static class NoOpCRLLoader implements CRLLoader {

        @Override
        public String id() {
            return "no-op-crl-loader";
        }

        @Override
        public void start() {
            // No-op
        }

        @Override
        public void stop() {
            // No-op
        }

        @Override
        public void setEventHandler(Consumer<List<CRL>> handler) {
            // No-op
        }
    }
}
