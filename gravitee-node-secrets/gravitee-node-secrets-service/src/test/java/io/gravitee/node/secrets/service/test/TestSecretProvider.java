package io.gravitee.node.secrets.service.test;

import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.plugin.SecretProvider;
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
    public Maybe<SecretMap> resolve(SecretURL secretURL) {
        return Maybe.just(secretMap(conf.getTestSecrets(), secretURL));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretURL secretURL) {
        Map<String, Object> created = conf.getTestSecrets();
        created.put("created_flag", "true");
        Map<String, Object> updated = conf.getTestSecrets();
        updated.put("updated_flag", "true");
        return Flowable.just(
            new SecretEvent(SecretEvent.Type.CREATED, secretMap(created, secretURL)),
            new SecretEvent(SecretEvent.Type.UPDATED, secretMap(updated, secretURL))
        );
    }

    private SecretMap secretMap(Map<String, ?> testSecrets, SecretURL secretURL) {
        return new SecretMap(
            testSecrets
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new Secret(entry.getValue(), entry.getKey().contains("base64"))))
        )
            .handleWellKnownSecretKeys(secretURL.wellKnowKeyMap());
    }
}
