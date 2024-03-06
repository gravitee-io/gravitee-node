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
package io.gravitee.node.vertx.client.ssl.jks;

import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.TrustStoreType;
import io.vertx.core.net.JksOptions;
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
public class JKSTrustStore extends TrustStore {

    @Serial
    private static final long serialVersionUID = -6603840868190194763L;

    private String path;
    private String content;
    private String password;
    private String alias;

    public JKSTrustStore() {
        super(TrustStoreType.JKS);
    }

    public JKSTrustStore(String path, String content, String password, String alias) {
        super(TrustStoreType.JKS);
        this.path = path;
        this.content = content;
        this.password = password;
        this.alias = alias;
    }

    @Override
    public Optional<TrustOptions> trustOptions() {
        final JksOptions jksOptions = new JksOptions();
        if (getPath() != null && !getPath().isEmpty()) {
            jksOptions.setPath(getPath());
        } else if (getContent() != null && !getContent().isEmpty()) {
            jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(getContent())));
        } else {
            throw new TrustOptionsException("Missing JKS truststore value");
        }

        jksOptions.setAlias(getAlias());
        jksOptions.setPassword(getPassword());
        return Optional.of(jksOptions);
    }
}
