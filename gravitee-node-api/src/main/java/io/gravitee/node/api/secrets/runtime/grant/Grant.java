package io.gravitee.node.api.secrets.runtime.grant;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
// TODO use CacheKey
public record Grant(String naturalId, String key) {}
