package io.gravitee.node.secrets.api.model;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretMount(String provider, SecretLocation location, SecretURL secretURL) {}
