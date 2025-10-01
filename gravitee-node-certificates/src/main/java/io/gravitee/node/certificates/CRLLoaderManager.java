package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLRefreshable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CRLLoaderManager {

    private final CRLRefreshable crlRefreshable;
    private final List<CRLLoader> crlLoaders = new ArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public CRLLoaderManager(CRLRefreshable crlRefreshable) {
        this.crlRefreshable = crlRefreshable;
    }

    public void registerCrlLoader(CRLLoader crlLoader) {
        if (crlLoader == null) {
            return;
        }
        log.info("Registering CRL loader: {}", crlLoader.getClass().getSimpleName());
        crlLoader.setEventHandler(crls -> {
            log.debug("CRL update received with {} CRL(s), refreshing trust manager", crls.size());
            crlRefreshable.refresh(crls);
        });
        crlLoaders.add(crlLoader);
    }

    public void start() {
        if (started.getAndSet(true)) {
            log.debug("CRL loaders already started, skipping");
            return;
        }
        log.info("Starting {} CRL loader(s)", crlLoaders.size());
        crlLoaders.forEach(CRLLoader::start);
    }

    public void stop() {
        log.info("Stopping {} CRL loader(s)", crlLoaders.size());
        crlLoaders.forEach(CRLLoader::stop);
    }
}
