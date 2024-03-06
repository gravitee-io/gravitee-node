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

import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.TrustStoreType;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.TrustOptions;
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
public class PEMTrustStore extends TrustStore {

    @Serial
    private static final long serialVersionUID = 7432939542056493096L;

    private String path;
    private String content;

    public PEMTrustStore() {
        super(TrustStoreType.PEM);
    }

    public PEMTrustStore(String path, String content) {
        super(TrustStoreType.PEM);
        this.path = path;
        this.content = content;
    }

    @Override
    public Optional<TrustOptions> trustOptions() {
        final PemTrustOptions pemTrustOptions = new PemTrustOptions();

        if (getPath() != null && !getPath().isEmpty()) {
            pemTrustOptions.addCertPath(getPath());
        } else if (getContent() != null && !getContent().isEmpty()) {
            pemTrustOptions.addCertValue(io.vertx.core.buffer.Buffer.buffer(getContent()));
        } else {
            throw new TrustOptionsException("Missing PEM certificate value");
        }

        return Optional.of(pemTrustOptions);
    }
}
