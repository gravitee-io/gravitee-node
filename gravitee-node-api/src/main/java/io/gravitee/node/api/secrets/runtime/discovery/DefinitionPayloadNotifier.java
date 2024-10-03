package io.gravitee.node.api.secrets.runtime.discovery;

import java.util.function.Consumer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DefinitionPayloadNotifier {
    void onPayload(String payload, PayloadLocation location, Consumer<String> updatedPayload);
}
