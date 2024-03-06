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
package io.gravitee.node.vertx.client.ssl.none;

import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.KeyStoreType;
import io.vertx.core.net.KeyCertOptions;
import java.io.Serial;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Data
public class NoneKeyStore extends KeyStore {

    @Serial
    private static final long serialVersionUID = -2540354913966457704L;

    public NoneKeyStore() {
        super(KeyStoreType.NONE);
    }

    @Override
    public Optional<KeyCertOptions> keyCertOptions() {
        // Nothing to do
        return Optional.empty();
    }
}
