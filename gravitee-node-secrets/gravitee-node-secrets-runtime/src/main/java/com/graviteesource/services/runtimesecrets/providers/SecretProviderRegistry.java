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
package com.graviteesource.services.runtimesecrets.providers;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.graviteesource.services.runtimesecrets.errors.SecretProviderNotFoundException;
import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SecretProviderRegistry {

    private final Multimap<String, SecretProviderEntry> perEnv = MultimapBuilder.hashKeys().arrayListValues().build();
    private final Map<String, SecretProvider> allEnvs = new ConcurrentHashMap<>();

    public void register(String id, SecretProvider provider, String envId) {
        if (envId == null || envId.isEmpty()) {
            allEnvs.put(id, provider);
        } else {
            synchronized (perEnv) {
                perEnv.put(envId, new SecretProviderEntry(id, provider));
            }
        }
    }

    /**
     *
     * @param envId environment ID
     * @param id is of the provider
     * @return a secret provider
     * @throws SecretProviderNotFoundException if the provider is not found
     */
    public Single<SecretProvider> get(String envId, String id) {
        return Maybe
            .defer(() -> {
                Collection<SecretProviderEntry> perEnvProviders;
                synchronized (perEnv) {
                    perEnvProviders = perEnv.get(envId);
                }

                return Maybe.fromOptional(
                    perEnvProviders
                        .stream()
                        .filter(entry -> entry.id().equals(id))
                        .map(SecretProviderEntry::provider)
                        .findFirst()
                        .or(() -> Optional.ofNullable(allEnvs.get(id)))
                );
            })
            .switchIfEmpty(Single.just(new AbstractSecretProviderDispatcher.ErrorSecretProvider()));
    }

    record SecretProviderEntry(String id, SecretProvider provider) {}
}
