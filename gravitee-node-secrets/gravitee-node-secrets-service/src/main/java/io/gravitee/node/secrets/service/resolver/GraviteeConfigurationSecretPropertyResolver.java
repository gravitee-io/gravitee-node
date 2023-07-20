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
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 * @since 3.9.11
 */
@Slf4j
public class GraviteeConfigurationSecretPropertyResolver implements PropertyResolver<Secret> {

    @Autowired
    private GraviteeConfigurationSecretResolverDispatcher dispatcher;

    private static final String SCHEME = "secrets://";

    @Override
    public boolean supports(String currentValue) {
        Objects.requireNonNull(currentValue);
        return dispatcher.enabledManagers().stream().anyMatch(manager -> currentValue.startsWith(String.format("%s%s/", SCHEME, manager)));
    }

    @Override
    public Maybe<Secret> resolve(String location) {
        return dispatcher.resolve(toSecretMount(location));
    }

    @Override
    public Flowable<Secret> watch(String location) {
        return dispatcher.watch(toSecretMount(location));
    }

    private SecretMount toSecretMount(String location) {
        String schemeLess = location.substring(SCHEME.length());
        String id = schemeLess.substring(0, schemeLess.indexOf('/'));
        return dispatcher
            .findSecretProvider(id)
            .map(secretProvider -> {
                try {
                    return secretProvider.fromURL(new URL(location));
                } catch (MalformedURLException e) {
                    throw new SecretManagerConfigurationException("cannot create secret URL from: " + location, e);
                }
            })
            .orElseThrow(() ->
                new SecretProviderNotFoundException(AbstractSecretProviderDispatcher.SECRET_PROVIDER_NOT_FOUND_FOR_ID.formatted(id))
            );
    }
}
