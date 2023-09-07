package io.gravitee.node.api.secrets.model;

import com.google.common.base.Strings;

/**
 * Represent where the secret is mounted.
 * It contains the provider id the location of the secret within that provider and optionally the key to fetch.
 * For traceability, it also contains the URL it was built from if it's the case.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretMount(String provider, SecretLocation location, String key, SecretURL secretURL) {
    /**
     * Test the precense of a key
     *
     * @return true is the key is present
     */
    public boolean isKeyEmpty() {
        return Strings.isNullOrEmpty(key);
    }
}
