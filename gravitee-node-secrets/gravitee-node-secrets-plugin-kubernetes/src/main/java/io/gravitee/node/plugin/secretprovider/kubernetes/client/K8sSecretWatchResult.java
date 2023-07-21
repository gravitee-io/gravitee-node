package io.gravitee.node.plugin.secretprovider.kubernetes.client;

import io.gravitee.node.secrets.api.model.SecretEvent;
import io.kubernetes.client.openapi.models.V1Secret;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record K8sSecretWatchResult(SecretEvent.Type type, V1Secret v1Secret) {}
