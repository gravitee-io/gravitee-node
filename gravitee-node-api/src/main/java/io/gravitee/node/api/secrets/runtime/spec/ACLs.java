package io.gravitee.node.api.secrets.runtime.spec;

import io.gravitee.common.secrets.ValueKind;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record ACLs(ValueKind valueKind, List<DefinitionACL> definitions, List<PluginACL> plugins) {
    public record DefinitionACL(String kind, List<String> ids) {}
    public record PluginACL(String id, List<String> fields) {}
}
