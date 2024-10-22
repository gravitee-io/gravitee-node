package io.gravitee.node.api.secrets.model;

import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;

/**
 * Represent a secret in the Secret Manager. It is a map key/secret.
 * It can have an pollInterval to help cache eviction.
 * <p>
 * Secrets can be pulled directly or using {@link WellKnownSecretKey} for well known secrets type (TLS and Basic Auth).
 * An explicit call to {@link #handleWellKnownSecretKeys(Map)} with a mapping must be performed to extract well-known keys.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode
public final class SecretMap {

    private final Map<String, Secret> map;
    private final Map<WellKnownSecretKey, Secret> wellKnown = new EnumMap<>(WellKnownSecretKey.class);
    private final Instant expireAt;

    /**
     * Create a {@link SecretMap} from a map of {@link Secret} without pollInterval
     *
     * @param map the map of {@link Secret}
     */
    public SecretMap(Map<String, Secret> map) {
        this(map, null);
    }

    /**
     * Create a {@link SecretMap} from a map of {@link Secret} with pollInterval
     *
     * @param map      the map of {@link Secret}
     * @param expireAt pollInterval
     */
    public SecretMap(Map<String, Secret> map, Instant expireAt) {
        this.map = map == null ? Map.of() : Map.copyOf(map);
        this.expireAt = expireAt;
    }

    /**
     *
     * @return a copy f the secrets
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
     * Builds a secret map where secrets are base64 encoded with pollInterval date
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
     * Builds a secret map with pollInterval date
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
     * @return optional of the pollInterval of this secret
     */
    public Optional<Instant> expireAt() {
        return Optional.ofNullable(expireAt);
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

    public SecretMap withExpireAt(Instant expireAt) {
        return new SecretMap(map, expireAt);
    }

    /**
     * Well-known field that can typically exist find in a secret. This is from Gravitee.io point of view.
     * Any consumer of those field should use {@link SecretMap#wellKnown(WellKnownSecretKey)} to use fetch the data.
     */
    public enum WellKnownSecretKey {
        CERTIFICATE,
        PRIVATE_KEY,
        USERNAME,
        PASSWORD,
    }
}
