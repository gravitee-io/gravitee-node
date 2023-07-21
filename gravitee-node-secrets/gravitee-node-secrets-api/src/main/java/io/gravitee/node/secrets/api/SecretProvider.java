/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.secrets.api;

import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.gravitee.node.secrets.api.model.SecretURL;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProvider {
    String PLUGIN_URL_SCHEME = "secret://";
    String PLUGIN_TYPE = "secret-provider";

    Maybe<Secret> resolve(SecretMount secretMount) throws SecretManagerException;

    Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events);

    SecretMount fromURL(SecretURL url);

    default SecretProvider start() throws SecretManagerException {
        return this;
    }

    default SecretProvider stop() {
        return this;
    }
}
