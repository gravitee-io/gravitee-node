package io.gravitee.node.secrets.plugins.internal.test;

import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;

public class TestSecretProvider implements SecretProvider {

    @Override
    public Maybe<SecretMap> resolve(SecretURL secretURL) {
        return Maybe.just(SecretMap.of(Map.of("password", "secret")));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretURL secretURL) {
        return Flowable.just(new SecretEvent(SecretEvent.Type.CREATED, SecretMap.of(Map.of("password", "secret"))));
    }
}
