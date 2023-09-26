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
package io.gravitee.node.api.secrets;

import io.gravitee.node.api.secrets.errors.SecretManagerException;
import io.gravitee.node.api.secrets.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * Base service implemented by the plugin. Instance of this class are created by {@link SecretProviderFactory} instance.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProvider {
    String PLUGIN_URL_SCHEME = "secret://";
    String PLUGIN_TYPE = "secret-provider";

    /**
     * Resolve a secret into and wrap it into a {@link Maybe}.
     * An error will be signaled if the secret is not found.
     *
     * @param secretMount where the secret is mounted.
     * @return a secret map (all keys of a secret)
     */
    Maybe<SecretMap> resolve(SecretMount secretMount);

    /**
     * Watches a secret. Will perform a {@link #resolve(SecretMount)} first before watching changes.
     *
     * @param secretMount where the secret is mounted.
     * @return a {@link Flowable} of event that contains the secret map of an empty secret map in case of deletion.
     */
    Flowable<SecretEvent> watch(SecretMount secretMount);

    /**
     * Turns a parsed URL into a {@link SecretMount}
     *
     * @param url the pasrsed url
     * @return a mount or raised an error
     * @throws IllegalArgumentException if the parsed url does not contain proper data to return a {@link SecretMount}
     */
    SecretMount fromURL(SecretURL url);

    /**
     * Performs startup logic if need be (is called once after a new instance is created)
     *
     * @return self
     * @throws SecretManagerException in case secret provider cannot be started
     */
    default SecretProvider start() throws SecretManagerException {
        return this;
    }

    /**
     * Stops what needs to be stopped when the plugin is no longer required or when the Gateway stops
     *
     * @return self
     */
    default SecretProvider stop() {
        return this;
    }
}
