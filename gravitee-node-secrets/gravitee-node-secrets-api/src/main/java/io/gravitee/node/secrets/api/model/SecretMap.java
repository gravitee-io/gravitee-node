package io.gravitee.node.secrets.api.model;

import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Represent a secret in the Secret Manager. It is a map key/secret.
 * It has when possible an expiration to help cache storage.
 * <p>
 * Secrets can be pulled directly or using {@link WellKnownSecretKey} for well know secrets type (TLS and Basic Auth).
 * An explicit call to {@link #handleWellKnownSecretKeys(Map)} with a mapping must be performed to extract well know keys.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public final class SecretMap {

    private final Map<String, Secret> map;
    private final Map<WellKnownSecretKey, Secret> wellKnown = new EnumMap<>(WellKnownSecretKey.class);
    private final Instant expireAt;

    public SecretMap(Map<String, Secret> map) {
        this(map, null);
    }

    public SecretMap(Map<String, Secret> map, Instant expireAt) {
        this.map = map == null ? Map.of() : Map.copyOf(map);
        this.expireAt = expireAt;
    }

    public static SecretMap of(Map<String, ?> data) {
        return new SecretMap(mapToMap(data));
    }

    public static SecretMap of(Map<String, ?> data, Instant expireAt) {
        return new SecretMap(mapToMap(data), expireAt);
    }

    private static Map<String, Secret> mapToMap(Map<String, ?> data) {
        return data.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> new Secret(e.getValue())));
    }

    public Optional<Secret> getSecret(SecretMount secretMount) {
        return Optional.ofNullable(map.get(secretMount.key()));
    }

    public Optional<Instant> expireAt() {
        return Optional.ofNullable(expireAt);
    }

    /**
     * Make well know key accessible via {@link #wellKnown(WellKnownSecretKey)}
     * The map must passed on at follows:
     * <li>key: the name if the well-known key inside the secret data</li>
     * <li>value: the matching {@link WellKnownSecretKey}</li>
     *
     * @param mapping the map describing the mapping
     */
    public void handleWellKnownSecretKeys(Map<String, WellKnownSecretKey> mapping) {
        map
                .entrySet()
                .stream()
                .filter(entry -> mapping.get(entry.getKey()) != null)
                .forEach(entry -> wellKnown.put(mapping.get(entry.getKey()), entry.getValue()));
    }

    public Optional<Secret> wellKnown(WellKnownSecretKey key) {
        return Optional.ofNullable(wellKnown.get(key));
    }

    public enum WellKnownSecretKey {
        CERTIFICATE,
        PRIVATE_KEY,
        USERNAME,
        PASSWORD,
    }
}
