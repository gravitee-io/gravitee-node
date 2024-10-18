package io.gravitee.node.api.secrets.runtime.grant;

import io.gravitee.node.api.secrets.runtime.spec.ValueKind;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record Grant(CacheKey cacheKey, String secretKey, ValueKind allowedKind, Set<String> allowedFields) {
    public boolean match(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return Boolean.TRUE;
        } else {
            Predicate<Grant> allowed = grant -> runtimeContext.allowed();
            Predicate<Grant> valueKindMatch = grant -> grant.allowedKind() == null || grant.allowedKind() == runtimeContext.allowKind();
            Predicate<Grant> noACLSFields = grant -> grant.allowedFields().isEmpty();
            Predicate<Grant> fieldMatch = grant -> grant.allowedFields().contains(runtimeContext.field().toLowerCase());
            return allowed.and(valueKindMatch).and(noACLSFields.or(fieldMatch)).test(this);
        }
    }
}
