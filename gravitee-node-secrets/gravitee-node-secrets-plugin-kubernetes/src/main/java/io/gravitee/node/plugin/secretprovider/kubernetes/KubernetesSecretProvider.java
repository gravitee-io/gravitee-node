package io.gravitee.node.plugin.secretprovider.kubernetes;

import io.gravitee.node.plugin.secretprovider.kubernetes.client.K8sClient;
import io.gravitee.node.plugin.secretprovider.kubernetes.config.K8sConfig;
import io.gravitee.node.plugin.secretprovider.kubernetes.config.K8sSecretLocation;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.*;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Secret;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Base64;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class KubernetesSecretProvider implements SecretProvider {

    public static final String PLUGIN_ID = "kubernetes";

    private final K8sClient client;

    public KubernetesSecretProvider(K8sConfig k8sConfig) {
        this.client = new K8sClient(k8sConfig);
    }

    @Override
    public Maybe<Secret> resolve(SecretMount secretMount) {
        try {
            K8sSecretLocation k8sLocation = K8sSecretLocation.fromLocation(secretMount.location());
            Optional<V1Secret> k8sSecret = client.getSecret(k8sLocation);

            return Maybe.fromOptional(
                k8sSecret
                    .map(V1Secret::getData)
                    .map(data -> data.get(k8sLocation.key()))
                    .map(Base64.getDecoder()::decode)
                    .map(decoded -> new Secret(decoded, null))
            );
        } catch (ApiException e) {
            return Maybe.error(new SecretManagerException(e));
        }
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events) {
        K8sSecretLocation k8sLocation = K8sSecretLocation.fromLocation(secretMount.location());
        return client
            .watchSecret(k8sLocation.namespace(), k8sLocation.secret())
            .flatMapMaybe(resp -> {
                if (resp.type() != SecretEvent.Type.DELETED) {
                    return Maybe.fromOptional(
                        Optional
                            .ofNullable(resp.v1Secret().getData())
                            .map(data -> data.get(k8sLocation.key()))
                            .map(Base64.getDecoder()::decode)
                            .map(decoded -> new SecretEvent(resp.type(), new Secret(decoded, null)))
                    );
                } else {
                    return Maybe.just(new SecretEvent(resp.type(), new Secret(new byte[0], null)));
                }
            });
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        K8sSecretLocation k8sSecretLocation = K8sSecretLocation.fromURL(url);
        return new SecretMount(url.provider(), new SecretLocation(k8sSecretLocation.asMap()), url);
    }
}
