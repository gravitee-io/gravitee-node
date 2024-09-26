package io.gravitee.node.api.secrets.runtime.discovery;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface DefinitionPayloadNotifier {
    void onPayload(String payload, PayloadLocation location);
}
