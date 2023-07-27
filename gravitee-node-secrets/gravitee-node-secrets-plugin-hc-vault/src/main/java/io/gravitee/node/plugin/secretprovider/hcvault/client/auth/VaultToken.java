package io.gravitee.node.plugin.secretprovider.hcvault.client.auth;

import java.time.Instant;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record VaultToken(String token, Instant expiry, boolean renewable) {}
