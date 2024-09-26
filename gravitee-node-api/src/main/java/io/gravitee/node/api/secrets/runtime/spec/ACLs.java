package io.gravitee.node.api.secrets.runtime.spec;

import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record ACLs(List<DefinitionACL> definitions, List<PluginACL> plugins) {
    public record DefinitionACL(String kind, List<String> ids) {}
    public record PluginACL(String id, List<String> fields) {}
}
