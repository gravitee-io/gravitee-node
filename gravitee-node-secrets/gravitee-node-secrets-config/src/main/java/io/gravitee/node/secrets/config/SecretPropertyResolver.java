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
package io.gravitee.node.secrets.config;

import io.gravitee.node.api.resolver.WatchablePropertyResolver;
import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretURL;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@NoArgsConstructor
public class SecretPropertyResolver implements WatchablePropertyResolver<Secret> {

    @Autowired
    private GraviteeConfigurationSecretResolver secretResolver;

    public SecretPropertyResolver(GraviteeConfigurationSecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Override
    public boolean supports(String value) {
        if (value == null) return false;
        return secretResolver.canResolveSingleValue(value);
    }

    @Override
    public Maybe<Secret> resolve(String location) {
        return secretResolver.resolveKey(secretResolver.asSecretURL(location));
    }

    @Override
    public boolean isWatchable(String value) {
        return SecretURL.from(value).isWatchable();
    }

    @Override
    public Flowable<Secret> watch(String location) {
        return secretResolver.watchKey(secretResolver.asSecretURL(location));
    }
}
