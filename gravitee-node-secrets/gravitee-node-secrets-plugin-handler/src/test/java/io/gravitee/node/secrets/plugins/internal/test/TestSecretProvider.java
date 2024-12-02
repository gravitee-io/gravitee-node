package io.gravitee.node.secrets.plugins.internal.test;

import io.gravitee.secrets.api.core.*;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;

public class TestSecretProvider implements SecretProvider {

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        return Maybe.just(SecretMap.of(Map.of("password", "secret")));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount) {
        return Flowable.just(new SecretEvent(SecretEvent.Type.CREATED, SecretMap.of(Map.of("password", "secret"))));
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        return new SecretMount("test", new SecretLocation(), "password", url);
    }
}
