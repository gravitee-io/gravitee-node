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
package io.gravitee.node.secrets.service.resolver;

import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.errors.SecretProviderNotFoundException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.gravitee.node.secrets.api.model.SecretURL;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class GraviteeConfigurationSecretPropertyResolver implements WatchablePropertyResolver<Secret> {

    @Autowired
    private GraviteeConfigurationSecretResolverDispatcher dispatcher;

    @Override
    public boolean supports(String value) {
        Objects.requireNonNull(value);
        return dispatcher
            .enabledManagers()
            .stream()
            .anyMatch(manager -> value.startsWith(String.format("%s%s/", SecretURL.SCHEME, manager)));
    }

    @Override
    public Maybe<Secret> resolve(String location) {
        return dispatcher.resolve(toSecretLocation(location));
    }

    @Override
    public boolean isWatchable(String value) {
        return SecretURL.from(value).isWatchable();
    }

    @Override
    public Flowable<Secret> watch(String location) {
        return dispatcher.watch(toSecretLocation(location));
    }

    private SecretMount toSecretLocation(String location) {
        SecretURL url = SecretURL.from(location);
        return dispatcher
            .findSecretProvider(url.provider())
            .map(secretProvider -> {
                try {
                    return secretProvider.fromURL(url);
                } catch (IllegalArgumentException e) {
                    throw new SecretManagerConfigurationException("cannot create secret URL from: " + location, e);
                }
            })
            .orElseThrow(() ->
                new SecretProviderNotFoundException(
                    AbstractSecretProviderDispatcher.SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(url.provider())
                )
            );
    }
}
