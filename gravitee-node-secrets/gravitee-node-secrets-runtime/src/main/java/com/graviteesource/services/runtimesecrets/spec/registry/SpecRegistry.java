package com.graviteesource.services.runtimesecrets.spec.registry;

import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
class SpecRegistry {

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
                return byUriAndKey.get(query.mainExpression().value());
            }
            return byUri.get(query.mainExpression().value());
        }
        return null;
    }

    Spec fromSpec(Spec query) {
        if (query.id() != null) {
            return byID.get(query.id());
        } else if (query.name() != null) {
            return byName.get(query.name());
        } else if (query.uri() != null) {
            if (query.key() != null) {
                return byUriAndKey.get(query.uriAndKey());
            }
            return byUri.get(query.uri());
        }
        return null;
    }
}
