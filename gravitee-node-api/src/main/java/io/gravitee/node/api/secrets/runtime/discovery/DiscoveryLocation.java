package io.gravitee.node.api.secrets.runtime.discovery;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record DiscoveryLocation(Definition definition, PayloadLocation... payloadLocations) {
    public record Definition(String kind, String id) {}
}
