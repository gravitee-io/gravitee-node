/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.vertx.client.ssl.pem;

import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.KeyStoreType;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import java.io.Serial;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
public class PEMKeyStore extends KeyStore {

    @Serial
    private static final long serialVersionUID = 1051430527272519608L;

    private String keyPath;
    private String keyContent;
    private String certPath;
    private String certContent;

    public PEMKeyStore() {
        super(KeyStoreType.PEM);
    }

    public PEMKeyStore(String keyPath, String keyContent, String certPath, String certContent) {
        super(KeyStoreType.PEM);
        this.keyPath = keyPath;
        this.keyContent = keyContent;
        this.certPath = certPath;
        this.certContent = certContent;
    }

    @Override
    public Optional<KeyCertOptions> keyCertOptions() {
        final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

        if (getCertPath() != null && !getCertPath().isEmpty()) {
            pemKeyCertOptions.setCertPath(getCertPath());
        } else if (getCertContent() != null && !getCertContent().isEmpty()) {
            pemKeyCertOptions.setCertValue(io.vertx.core.buffer.Buffer.buffer(getCertContent()));
        } else {
            throw new KeyStoreCertOptionsException("Missing PEM certificate value");
        }

        if (getKeyPath() != null && !getKeyPath().isEmpty()) {
            pemKeyCertOptions.setKeyPath(getKeyPath());
        } else if (getKeyContent() != null && !getKeyContent().isEmpty()) {
            pemKeyCertOptions.setKeyValue(io.vertx.core.buffer.Buffer.buffer(getKeyContent()));
        } else {
            throw new KeyStoreCertOptionsException("Missing PEM key value");
        }

        return Optional.of(pemKeyCertOptions);
    }
}
