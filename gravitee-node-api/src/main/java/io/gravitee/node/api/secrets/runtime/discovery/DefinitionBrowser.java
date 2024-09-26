package io.gravitee.node.api.secrets.runtime.discovery;

import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DefinitionBrowser<T> {
    boolean canHandle(T definition);

    Definition getDefinitionKindLocation(T definition, Map<String, String> metadata);

    void findPayloads(DefinitionPayloadNotifier notifier);
}
