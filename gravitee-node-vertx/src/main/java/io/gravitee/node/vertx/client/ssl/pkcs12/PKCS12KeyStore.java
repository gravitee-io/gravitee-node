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

import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.KeyStoreType;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PfxOptions;
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
public class PKCS12KeyStore extends KeyStore {

    @Serial
    private static final long serialVersionUID = 1210626721233767960L;

    private String path;
    private String content;
    private String password;
    private String alias;

    /**
     * For PKCS12, if specified, it must be equals to password as the JDK doesn't support a key password different from the keystore password.
     */
    private String keyPassword;

    public PKCS12KeyStore() {
        super(KeyStoreType.PKCS12);
    }

    public PKCS12KeyStore(String path, String content, String password, String alias, String keyPassword) {
        super(KeyStoreType.PKCS12);
        this.path = path;
        this.content = content;
        this.password = password;
        this.alias = alias;
        this.keyPassword = keyPassword;
    }

    @Override
    public Optional<KeyCertOptions> keyCertOptions() {
        final PfxOptions pfxOptions = new PfxOptions();

        if (getPath() != null && !getPath().isEmpty()) {
            pfxOptions.setPath(getPath());
        } else if (getContent() != null && !getContent().isEmpty()) {
            pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(getContent())));
        } else {
            throw new KeyStoreCertOptionsException("Missing PKCS12 keystore value");
        }

        pfxOptions.setAlias(getAlias());
        pfxOptions.setAliasPassword(getKeyPassword());
        pfxOptions.setPassword(getPassword());
        return Optional.of(pfxOptions);
    }
}
