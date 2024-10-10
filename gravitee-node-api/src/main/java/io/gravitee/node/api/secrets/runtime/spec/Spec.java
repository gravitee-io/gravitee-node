package io.gravitee.node.api.secrets.runtime.spec;

import static io.gravitee.node.api.secrets.runtime.discovery.Ref.formatUriAndKey;

import io.gravitee.node.api.secrets.model.SecretURL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Spec(
    String id,
    String name,
    String uri, // /vault/secrets/passwords
    String key, // 1/ redis, 2/ ldap
    List<ChildSpec> children,
    boolean usesDynamicKey,
    boolean isOnTheFly,
    Resolution resolution,
    ACLs acls,
    String envId
) {
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public Optional<ChildSpec> findChildrenFromName(String query) {
        if (hasChildren()) {
            return children.stream().filter(child -> Objects.equals(child.name(), query)).findFirst();
        }
        return Optional.empty();
    }

    public Optional<ChildSpec> findChildrenFromUri(String query) {
        if (hasChildren()) {
            return children.stream().filter(child -> Objects.equals(child.uri(), query)).findFirst();
        }
        return Optional.empty();
    }

    public String uriAndKey() {
        return formatUriAndKey(uri, key);
    }

    public SecretURL toSecretURL() {
        return SecretURL.from(uriAndKey(), false);
    }

    public String naturalId() {
        return name != null && !name.isEmpty() ? name : uri;
    }

    public record ChildSpec(String name, String uri, String key) {}

    public boolean hasResolutionType(Resolution.Type type) {
        if (type == Resolution.Type.ONCE) {
            return resolution == null || resolution.type() == Resolution.Type.ONCE;
        }
        return resolution != null && type.equals(resolution.type());
    }
}
