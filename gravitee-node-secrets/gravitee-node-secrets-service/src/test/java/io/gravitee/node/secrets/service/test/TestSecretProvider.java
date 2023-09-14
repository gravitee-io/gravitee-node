package io.gravitee.node.secrets.service.test;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.stream.Collectors;

public class TestSecretProvider implements SecretProvider {

    private final TestSecretProviderConfiguration conf;

    public TestSecretProvider(TestSecretProviderConfiguration configuration) {
        this.conf = configuration;
    }

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        return Maybe.just(secretMap(conf.getTestSecrets(), secretMount));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount) {
        return Flowable.just(
            new SecretEvent(SecretEvent.Type.CREATED, secretMap(conf.getTestSecrets(), secretMount)),
            new SecretEvent(SecretEvent.Type.UPDATED, secretMap(conf.getTestSecrets(), secretMount))
        );
    }

    private SecretMap secretMap(Map<String, ?> testSecrets, SecretMount secretMount) {
        return new SecretMap(
            testSecrets
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new Secret(entry.getValue(), entry.getKey().contains("base64"))))
        )
            .handleWellKnownSecretKeys(secretMount.secretURL().wellKnowKeyMap());
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        if (!url.path().equals("test")) {
            throw new IllegalArgumentException();
        }
        return new SecretMount(url.provider(), new SecretLocation(Map.of("path", url.path())), url.key(), url);
    }
}
