package io.gravitee.node.api.secrets.model;

import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;

/**
 * Represent a secret in the Secret Manager. It is a map key/secret.
 * It can have an expiration to help cache eviction.
 * <p>
 * Secrets can be pulled directly or using {@link WellKnownSecretKey} for well known secrets type (TLS and Basic Auth).
 * An explicit call to {@link #handleWellKnownSecretKeys(Map)} with a mapping must be performed to extract well-known keys.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode
public final class SecretMap implements WithExpiration {

    private final Map<String, Secret> map;
    private final Map<WellKnownSecretKey, Secret> wellKnown = new EnumMap<>(WellKnownSecretKey.class);
    private final Instant expiresAt;

    /**
     * Create a {@link SecretMap} from a map of {@link Secret} without expiration
     *
     * @param map the map of {@link Secret}
     */
    public SecretMap(Map<String, Secret> map) {
        this(map, null);
    }

    /**
     * Create a {@link SecretMap} from a map of {@link Secret} with expiration
     *
     * @param map      the map of {@link Secret}
     * @param expiresAt expiration
     */
    public SecretMap(Map<String, Secret> map, Instant expiresAt) {
        this.map = map == null ? Map.of() : Map.copyOf(map);
        this.expiresAt = expiresAt;
    }

    /**
     *
     * @return a copy of the secrets as immutable map
     */
    public Map<String, Secret> asMap() {
        return Map.copyOf(map);
    }

    /**
     * Builds a secret map where secrets are base64 encoded
     *
     * @param data the secret as a map (String/byte[] or String/String) where bytes or String are base64 encoded
     * @return a {@link SecretMap}
     * @see Secret#Secret(Object)
     */
    public static SecretMap ofBase64(Map<String, ?> data) {
        return new SecretMap(mapToMap(data, true));
    }

    /**
     * Builds a secret map where secrets are base64 encoded with expiration date
     *
     * @param data     the secret as a map (String/byte[] or String/String) where bytes or String are base64 encoded
     * @param expireAt when the secret expires
     * @return a {@link SecretMap}
     * @see Secret#Secret(Object)
     */
    public static SecretMap ofBase64(Map<String, ?> data, Instant expireAt) {
        return new SecretMap(mapToMap(data, true), expireAt);
    }

    /**
     * Builds a secret map
     *
     * @param data the secret as a map (String/byte[] or String/String)
     * @return a {@link SecretMap}
     * @see Secret#Secret(Object)
     */
    public static SecretMap of(Map<String, ?> data) {
        return new SecretMap(mapToMap(data, false));
    }

    /**
     * Builds a secret map with expiration date
     *
     * @param data     the secret as a map (String/byte[] or String/String)
     * @param expireAt when the secret expires
     * @return a {@link SecretMap}
     * @see Secret#Secret(Object)
     */
    public static SecretMap of(Map<String, ?> data, Instant expireAt) {
        return new SecretMap(mapToMap(data, false), expireAt);
    }

    private static Map<String, Secret> mapToMap(Map<String, ?> data, boolean base64) {
        return data.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> new Secret(e.getValue(), base64)));
    }

    /**
     * Get a secret from the map using the {@link SecretMount#key()}
     *
     * @param secretMount the mount to use
     * @return optional of a required secret
     */
    public Optional<Secret> getSecret(SecretMount secretMount) {
        return Optional.ofNullable(map.get(secretMount.key()));
    }

    /**
     * @return optional of the expiration of this secret
     */
    public Optional<Instant> expiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * Make well-know key accessible via {@link #wellKnown(WellKnownSecretKey)}
     * The map must be passed on as follows:
     * <li>key: the name of well-known key inside the secret data</li>
     * <li>value: the matching {@link WellKnownSecretKey}</li>
     * if the key in the secret is not found, it will be ignored
     *
     * @param mapping the map describing the mapping
     * @return this updated {@link SecretMap} instance
     */
    public SecretMap handleWellKnownSecretKeys(Map<String, WellKnownSecretKey> mapping) {
        map
            .entrySet()
            .stream()
            .filter(entry -> mapping.get(entry.getKey()) != null)
            .forEach(entry -> wellKnown.put(mapping.get(entry.getKey()), entry.getValue()));
        return this;
    }

    /**
     * Retrieve a well-known field in a secret as an option.
     *
     * @param key the key to return
     * @return optional of a secret.
     */
    public Optional<Secret> wellKnown(WellKnownSecretKey key) {
        return Optional.ofNullable(wellKnown.get(key));
    }

    /**
     * Compute a new secret map with expiration. If the <code>secretMount</code> has a key,
     * then only the secret matching that key will be set to expire. If not the whole map is set to expire.
     * @param secretMount the secret mount used to fetch that secret
     * @param expireAt the expiration instant
     * @return a new {@link SecretMap} containing expiring secrets
     */
    public SecretMap withExpiresAt(SecretMount secretMount, Instant expireAt) {
        if (secretMount.isKeyEmpty()) {
            // the whole map can expire
            return new SecretMap(this.asMap(), expireAt);
        } else {
            Optional<Secret> expiring = this.getSecret(secretMount).map(secret -> secret.withExpiresAt(expireAt));
            // set the secret to expire
            if (expiring.isPresent()) {
                Map<String, Secret> secrets = new HashMap<>(this.asMap());
                secrets.put(secretMount.key(), expiring.get());
                return new SecretMap(secrets);
            }
            return this;
        }
    }

    /**
     * Well-known field that can typically exist find in a secret. This is from Gravitee.io point of view.
     * Any consumer of those field should use {@link SecretMap#wellKnown(WellKnownSecretKey)} to fetch the data.
     */
    public enum WellKnownSecretKey {
        CERTIFICATE,
        PRIVATE_KEY,
        USERNAME,
        PASSWORD,
    }
}
