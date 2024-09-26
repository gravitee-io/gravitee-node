package io.gravitee.node.api.secrets.runtime.spec;

import static io.gravitee.node.api.secrets.runtime.discovery.Ref.URI_KEY_SEPARATOR;

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
    String uri,
    String key,
    List<ChildSpec> children,
    boolean usesDynamicKey,
    boolean isRuntime,
    RenewalPolicy renewalPolicy,
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
        return uri + URI_KEY_SEPARATOR + key;
    }

    public SecretURL toSecretURL() {
        return SecretURL.from(uriAndKey(), false);
    }

    public String naturalId() {
        return name != null && !name.isEmpty() ? name : uri;
    }

    public record ChildSpec(String name, String uri, String key) {}
}
