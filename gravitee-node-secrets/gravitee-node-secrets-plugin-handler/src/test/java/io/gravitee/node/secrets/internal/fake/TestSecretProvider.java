package io.gravitee.node.secrets.internal.fake;

import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;

public class TestSecretProvider implements SecretProvider {

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        return Maybe.just(SecretMap.of(Map.of("password", "secret")));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events) {
        return Flowable.just(new SecretEvent(SecretEvent.Type.CREATED, SecretMap.of(Map.of("password", "secret"))));
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        return new SecretMount("test", new SecretLocation(), "password", url);
    }
}
