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
package io.gravitee.node.vertx.cert;

import io.gravitee.node.certificates.BaseKeyStoreManager;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxTrustStoreManager extends BaseKeyStoreManager {

    private final TrustOptions trustOptions;

    public VertxTrustStoreManager(String serverId) {
        super(serverId, false);
        this.trustOptions = new VertxTrustOptions(certManager);
    }

    /**
     * Get the corresponding {@link KeyCertOptions} that can be used to configure a vertx server.
     *
     * @return a {@link KeyCertOptions}.
     */
    public TrustOptions getTrustOptions() {
        return trustOptions;
    }
}
