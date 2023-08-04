package io.gravitee.node.secrets.api.model;

import com.google.common.base.Strings;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretMount(String provider, SecretLocation location, String key, SecretURL secretURL) {
    public boolean isKeyEmpty() {
        return Strings.isNullOrEmpty(key);
    }
}
