package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class MockSecretProvider implements SecretProvider {

    public static final String PLUGIN_ID = "mock";

    private final MockSecretProviderConfiguration configuration;

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        MockSecretLocation location = MockSecretLocation.fromLocation(secretMount.location());
        Map<String, Object> secretMap = ConfigHelper.removePrefix(configuration.getSecrets(), location.secret());
        if (secretMap.isEmpty()) {
            return Maybe.empty();
        }
        return Maybe.just(SecretMap.of(secretMap));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount) {
        return Flowable.empty();
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        return new SecretMount(PLUGIN_ID, MockSecretLocation.fromUrl(url), url.key(), url);
    }
}
