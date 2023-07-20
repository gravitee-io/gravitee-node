package io.gravitee.node.plugin.secretprovider.kubernetes;

import com.google.common.base.Splitter;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.node.plugin.secretprovider.kubernetes.config.KubernetesConfiguration;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.Secret;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.gravitee.node.secrets.api.model.SecretMount;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class KubernetesSecretProvider implements SecretProvider {

    private static final String PLUGIN_ID = "kubernetes";
    private final Splitter urlPathSplitter = Splitter.on('/');

    private final KubernetesConfiguration kubernetesConfiguration;

    public KubernetesSecretProvider(KubernetesConfiguration kubernetesConfiguration) {
        this.kubernetesConfiguration = kubernetesConfiguration;
    }

    @Override
    public Maybe<Secret> resolve(SecretMount secretMount) throws SecretManagerException {
        log.info("config: {}", this.kubernetesConfiguration);
        log.info("resolving secret: {}", secretMount);
        return Maybe.just(new Secret("gravitee".getBytes(StandardCharsets.UTF_8), null));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount mount, SecretEvent.Type... events) {
        return Flowable.error(new OnErrorNotImplementedException(new NoSuchMethodException("not implemented")));
    }

    @Override
    public SecretMount fromURL(URL url) {
        String path = url.getPath();
        List<String> elements = urlPathSplitter.splitToList(path);
        if (elements.size() == 4) {
            Assert.isTrue(PLUGIN_ID.equals(elements.get(0)), "url path does not start with %s".formatted(PLUGIN_ID));
            return new SecretMount(
                elements.get(0),
                url,
                Map.of(
                    KubernetesConfiguration.LOCATION_NAMESPACE,
                    elements.get(1),
                    KubernetesConfiguration.LOCATION_SECRET,
                    elements.get(2),
                    KubernetesConfiguration.LOCATION_FIELD,
                    elements.get(3)
                )
            );
        }
        throw new SecretManagerConfigurationException(
            "URL '%s' is not valid for Kubernetes plugin. Should be secrets://kubernetes/<namespace>/<secret name>/<field in secret>".formatted(
                    url
                )
        );
    }
}
