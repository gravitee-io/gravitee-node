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

import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.client.ssl.TrustStoreType;
import io.vertx.core.net.TrustOptions;
import java.io.Serial;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Data
public class NoneTrustStore extends TrustStore {

    @Serial
    private static final long serialVersionUID = -6013813999148592319L;

    public NoneTrustStore() {
        super(TrustStoreType.NONE);
    }

    @Override
    public Optional<TrustOptions> trustOptions() {
        // Nothing to do
        return Optional.empty();
    }
}
