/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.certificates;

import io.gravitee.node.api.certificate.CRLLoader;
import io.gravitee.node.api.certificate.CRLRefreshable;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Manager responsible for managing a {@link CRLLoader} instance and propagating CRL updates
 * to a {@link CRLRefreshable} component. It acts as a lifecycle manager for the CRL loader.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class CRLLoaderManager {

    private final CRLRefreshable crlRefreshable;
    private CRLLoader crlLoader;
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
        this.crlLoader = crlLoader;
    }

    public void start() {
        if (started.getAndSet(true)) {
            log.debug("CRL loader already started, skipping");
            return;
        }
        if (crlLoader != null) {
            log.info("Starting CRL loader");
            crlLoader.start();
        }
    }

    public void stop() {
        if (crlLoader != null) {
            log.info("Stopping CRL loader");
            crlLoader.stop();
        }
    }
}
