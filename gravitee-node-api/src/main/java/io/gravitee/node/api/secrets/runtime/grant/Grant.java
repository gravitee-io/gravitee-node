package io.gravitee.node.api.secrets.runtime.grant;

import io.gravitee.node.api.secrets.runtime.storage.CacheKey;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record Grant(CacheKey cacheKey, String secretKey) {}
