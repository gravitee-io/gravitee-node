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
package com.graviteesource.services.runtimesecrets.el;

import static io.gravitee.node.api.secrets.runtime.discovery.Ref.URI_KEY_SEPARATOR;

import com.graviteesource.services.runtimesecrets.discovery.RefParser;
import com.graviteesource.services.runtimesecrets.errors.*;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryLocation;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.grant.Grant;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class Service {

    private final Cache cache;
    private final GrantService grantService;
    private final SpecLifecycleService specLifecycleService;
    private final SpecRegistry specRegistry;

    public String fromGrant(String contextId, String envId) {
        Optional<Grant> grantOptional = grantService.getGrant(contextId);
        if (grantOptional.isEmpty()) {
            return resultToValue(new Result(Result.Type.DENIED, "secret was denied ahead of traffic"));
        }
        return getFromCache(envId, grantOptional.get(), grantOptional.get().key());
    }

    public String fromGrant(String contextId, String envId, String key) {
        Optional<Grant> grantOptional = grantService.getGrant(contextId);
        if (grantOptional.isEmpty()) {
            return resultToValue(new Result(Result.Type.DENIED, "secret was denied ahead of traffic"));
        }
        return getFromCache(envId, grantOptional.get(), key);
    }

    private String getFromCache(String envId, Grant grant, String key) {
        return resultToValue(
            toResult(
                cache
                    .get(envId, grant.naturalId())
                    .orElse(
                        new Entry(
                            Entry.Type.EMPTY,
                            null,
                            "no value in cache for [%s] in environment [%s]".formatted(grant.naturalId(), envId)
                        )
                    ),
                key
            )
        );
    }

    public String fromGrantWithName(String token, String envId, String name, String childName) {
        return null;
    }

    public String fromGrantWithUri(String token, String envId, String name, String childUri) {
        return null;
    }

    public String fromELWithUri(String envId, String uriWithKey, String definitionKind, String definitionId) {
        if (uriWithKey.contains(URI_KEY_SEPARATOR)) {
            RefParser.UriAndKey uriAndKey = RefParser.parseUriAndKey(uriWithKey);
            Ref ref = uriAndKey.asRef();
            Spec spec = specRegistry.getFromUriAndKey(envId, uriWithKey);
            if (spec == null && specLifecycleService.shouldDeployOnTheFly(ref)) {
                spec = specLifecycleService.deployOnTheFly(envId, ref);
            }
            return grantAndGet(envId, definitionKind, definitionId, spec, ref, uriAndKey.uri(), uriAndKey.key());
        } else {
            return resultToValue(new Result(Result.Type.ERROR, "uri must contain a key like such: /provider/uri:key"));
        }
    }

    public String fromELWithName(String envId, String name, String definitionKind, String definitionId) {
        Ref ref = new Ref(Ref.MainType.NAME, new Ref.Expression(name, false), null, null, name);
        Spec spec = specRegistry.getFromName(envId, name);
        return grantAndGet(envId, definitionKind, definitionId, spec, ref, name, spec.key());
    }

    private String grantAndGet(String envId, String definitionKind, String definitionId, Spec spec, Ref ref, String naturalId, String key) {
        boolean granted = grantService.grant(
            new DiscoveryContext(null, envId, ref, new DiscoveryLocation(new DiscoveryLocation.Definition(definitionKind, definitionId))),
            spec
        );
        if (!granted) {
            resultToValue(new Result(Result.Type.DENIED, "secret [%s] is denied in environment [%s]".formatted(naturalId, envId)));
        }
        return resultToValue(
            toResult(
                cache
                    .get(envId, naturalId)
                    .orElse(
                        new Entry(Entry.Type.EMPTY, null, "no value in cache for [%s] in environment [%s]".formatted(naturalId, envId))
                    ),
                key
            )
        );
    }

    private Result toResult(Entry entry, String key) {
        Result result;
        switch (entry.type()) {
            case VALUE -> {
                Map<String, Secret> secretMap = entry.value();
                Secret secret = secretMap.get(key);
                if (secret != null) {
                    result = new Result(Result.Type.VALUE, secret.asString());
                } else {
                    result = new Result(Result.Type.KEY_NOT_FOUND, "key [%s] not found".formatted(key));
                }
            }
            case EMPTY -> {
                result = new Result(Result.Type.EMPTY, entry.error());
            }
            case NOT_FOUND -> {
                result = new Result(Result.Type.NOT_FOUND, entry.error());
            }
            case ERROR -> {
                result = new Result(Result.Type.ERROR, entry.error());
            }
            default -> result = null;
        }
        return result;
    }

    private String resultToValue(Result result) {
        if (result != null) {
            switch (result.type()) {
                case VALUE -> {
                    return result.value();
                }
                case NOT_FOUND -> throw new SecretNotFoundException(result.value());
                case KEY_NOT_FOUND -> throw new SecretKeyNotFoundException(result.value());
                case EMPTY -> throw new SecretEmptyException(result.value());
                case ERROR -> throw new SecretProviderException(result.value());
                case DENIED -> throw new SecretAccessDeniedException(result.value());
            }
        }
        return null;
    }
}
