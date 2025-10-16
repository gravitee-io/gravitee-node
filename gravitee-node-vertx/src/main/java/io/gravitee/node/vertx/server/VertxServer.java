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
package io.gravitee.node.vertx.server;

import io.gravitee.node.api.server.Server;
import io.gravitee.node.certificates.CRLLoaderManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.certificates.TrustStoreLoaderManager;
import io.vertx.rxjava3.core.Vertx;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public abstract class VertxServer<T, C extends VertxServerOptions> implements Server<C> {

    protected final Vertx vertx;
    protected final C options;
    protected final KeyStoreLoaderManager keyStoreLoaderManager;
    protected final TrustStoreLoaderManager trustStoreLoaderManager;
    protected final CRLLoaderManager crlLoaderManager;
    protected final List<T> delegates = new CopyOnWriteArrayList<>();

    @Override
    public String id() {
        return options.getId();
    }

    public abstract T newInstance();

    public abstract List<T> instances();

    public final void startKeyStoreManagers() {
        try {
            keyStoreLoaderManager.start();
            trustStoreLoaderManager.start();
            crlLoaderManager.start();
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("cannot start key store managers", e);
        }
    }

    public void stop() {
        try {
            keyStoreLoaderManager.stop();
            trustStoreLoaderManager.stop();
            crlLoaderManager.stop();
        } catch (Exception e) {
            log.error("error stopping key store managers", e);
        }
    }

    public KeyStoreLoaderManager keyStoreLoaderManager() {
        return keyStoreLoaderManager;
    }

    public TrustStoreLoaderManager trustStoreLoaderManager() {
        return trustStoreLoaderManager;
    }
}
