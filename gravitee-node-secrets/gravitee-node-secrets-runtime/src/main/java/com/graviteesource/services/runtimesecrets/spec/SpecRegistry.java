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
package com.graviteesource.services.runtimesecrets.spec;

import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SpecRegistry {

    private final Map<String, Registry> registries = new HashMap<>();

    public void register(Spec spec) {
        registry(spec.envId()).register(spec);
    }

    public void unregister(Spec spec) {
        registry(spec.envId()).unregister(spec);
    }

    public void replace(Spec oldSpec, Spec newSpec) {
        String envId = oldSpec.envId();
        synchronized (registry(envId)) {
            registry(envId).unregister(oldSpec);
            registry(envId).register(newSpec);
        }
    }

    public Spec getFromName(String envId, String name) {
        return registry(envId).getFromName(name);
    }

    public Spec getFromUri(String envId, String uri) {
        return registry(envId).getFromUri(uri);
    }

    public Spec getFromUriAndKey(String envId, String uriAndKey) {
        return registry(envId).getFromUriAndKey(uriAndKey);
    }

    public Spec getFromID(String envId, String id) {
        return registry(envId).getFromID(id);
    }

    public Spec fromSpec(String envId, Spec query) {
        return registry(envId).fromSpec(query);
    }

    public Spec fromRef(String envId, Ref query) {
        return registry(envId).fromRef(query);
    }

    private Registry registry(String envId) {
        return registries.computeIfAbsent(envId, ignore -> new Registry());
    }

    private static class Registry {

        private final Map<String, Spec> byName = new HashMap<>();
        private final Map<String, Spec> byUri = new HashMap<>();
        private final Map<String, Spec> byUriAndKey = new HashMap<>();
        private final Map<String, Spec> byID = new HashMap<>();

        void register(Spec spec) {
            if (spec.id() != null) {
                byID.put(spec.id(), spec);
            }
            if (spec.name() != null) {
                byName.put(spec.name(), spec);
            }
            if (spec.uri() != null) {
                byUri.put(spec.uri(), spec);
                if (spec.key() != null) {
                    byUriAndKey.put(spec.uriAndKey(), spec);
                }
            }
        }

        void unregister(Spec spec) {
            if (spec.id() != null) {
                byID.remove(spec.id());
            }
            if (spec.uri() != null) {
                byUri.remove(spec.uri());
                if (spec.key() != null) {
                    byUriAndKey.remove(spec.uriAndKey(), spec);
                }
            }
            if (spec.name() != null) {
                byName.remove(spec.name());
            }
        }

        Spec getFromName(String name) {
            return byName.get(name);
        }

        Spec getFromUri(String uri) {
            return byUri.get(uri);
        }

        Spec getFromUriAndKey(String uriAndKey) {
            return byUriAndKey.get(uriAndKey);
        }

        Spec getFromID(String id) {
            return byID.get(id);
        }

        Spec fromRef(Ref query) {
            if (query.mainType() == Ref.MainType.NAME) {
                return byName.get(query.mainExpression().value());
            }
            if (query.mainType() == Ref.MainType.URI) {
                if (query.secondaryType() == Ref.SecondaryType.KEY) {
                    return byUriAndKey.get(query.uriAndKey());
                }
                return byUri.get(query.mainExpression().value());
            }
            return null;
        }

        Spec fromSpec(Spec query) {
            Spec result = null;
            if (query.id() != null) {
                result = byID.get(query.id());
            }
            if (result == null && query.name() != null) {
                result = byName.get(query.name());
            }
            if (result == null && query.uri() != null) {
                if (query.key() != null) {
                    result = byUriAndKey.get(query.uriAndKey());
                } else {
                    result = byUri.get(query.uri());
                }
            }
            return result;
        }
    }
}
