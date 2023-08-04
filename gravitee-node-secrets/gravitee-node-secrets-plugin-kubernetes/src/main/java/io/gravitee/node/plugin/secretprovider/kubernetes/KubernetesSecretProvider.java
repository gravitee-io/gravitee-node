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
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class KubernetesSecretProvider implements SecretProvider {

    private static final Map<String, SecretMap.WellKnownSecretKey> DEFAULT_WELL_KNOW_KEY_MAP = Map.of(
        "tls.crt",
        SecretMap.WellKnownSecretKey.CERTIFICATE,
        "tls.key",
        SecretMap.WellKnownSecretKey.PRIVATE_KEY
    );
    public static final String PLUGIN_ID = "kubernetes";

    private final K8sClient client;

    public KubernetesSecretProvider(K8sConfig k8sConfig) {
        this.client = new K8sClient(k8sConfig);
    }

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        try {
            K8sSecretLocation k8sLocation = K8sSecretLocation.fromLocation(secretMount.location());
            Optional<V1Secret> k8sSecret = client.getSecret(k8sLocation);
            return Maybe
                .fromOptional(k8sSecret.map(V1Secret::getData).map(SecretMap::of))
                .doOnSuccess(map -> handleWellKnowSecretKey(map, secretMount));
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
                    return Maybe
                        .fromOptional(
                            Optional.ofNullable(resp.v1Secret().getData()).map(data -> new SecretEvent(resp.type(), SecretMap.of(data)))
                        )
                        .doOnSuccess(event -> handleWellKnowSecretKey(event.secretMap(), secretMount));
                } else {
                    return Maybe.just(new SecretEvent(resp.type(), new SecretMap(null)));
                }
            });
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        K8sSecretLocation k8sSecretLocation = K8sSecretLocation.fromURL(url);
        return new SecretMount(url.provider(), new SecretLocation(k8sSecretLocation.asMap()), k8sSecretLocation.key(), url);
    }

    private void handleWellKnowSecretKey(SecretMap secretMap, SecretMount secretMount) {
        secretMap.handleWellKnownSecretKeys(
            Optional
                .ofNullable(secretMount.secretURL())
                .map(SecretURL::wellKnowKeyMap)
                .filter(MapUtils::isNotEmpty)
                .orElse(DEFAULT_WELL_KNOW_KEY_MAP)
        );
    }
}
