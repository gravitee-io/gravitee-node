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
package io.gravitee.node.vertx.client.ssl.pkcs12;

import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.TrustStoreType;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TrustOptions;
import java.io.Serial;
import java.util.Base64;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
public class PKCS12TrustStore extends TrustStore {

    @Serial
    private static final long serialVersionUID = 3915578060196536545L;

    private String path;
    private String content;
    private String password;
    private String alias;

    public PKCS12TrustStore() {
        super(TrustStoreType.PKCS12);
    }

    public PKCS12TrustStore(String path, String content, String password, String alias) {
        super(TrustStoreType.PKCS12);
        this.path = path;
        this.content = content;
        this.password = password;
        this.alias = alias;
    }

    @Override
    public Optional<TrustOptions> trustOptions() {
        final PfxOptions pfxOptions = new PfxOptions();

        if (getPath() != null && !getPath().isEmpty()) {
            pfxOptions.setPath(getPath());
        } else if (getContent() != null && !getContent().isEmpty()) {
            pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(getContent())));
        } else {
            throw new TrustOptionsException("Missing PKCS12 truststore value");
        }

        pfxOptions.setAlias(getAlias());
        pfxOptions.setPassword(getPassword());
        return Optional.of(pfxOptions);
    }
}
