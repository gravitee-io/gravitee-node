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
package io.gravitee.node.vertx.client.ssl;

import io.vertx.core.net.TrustOptions;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import lombok.Getter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
public abstract class TrustStore implements Serializable {

    @Serial
    private static final long serialVersionUID = -9209765483153309314L;

    private final TrustStoreType type;

    protected TrustStore(TrustStoreType type) {
        this.type = type;
    }

    public abstract Optional<TrustOptions> trustOptions();

    public static class TrustOptionsException extends RuntimeException {

        public TrustOptionsException(String message) {
            super(message);
        }
    }
}
