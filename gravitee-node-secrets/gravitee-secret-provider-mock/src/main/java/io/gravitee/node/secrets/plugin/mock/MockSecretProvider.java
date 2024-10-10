package io.gravitee.node.secrets.plugin.mock;

import static io.gravitee.plugin.core.internal.AbstractPluginEventListener.SECRET_PROVIDER;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.api.secrets.model.SecretURL;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import io.gravitee.node.secrets.plugin.mock.conf.ConfiguredError;
import io.gravitee.node.secrets.plugin.mock.conf.ConfiguredEvent;
import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import io.gravitee.node.secrets.plugin.mock.conf.Renewal;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class MockSecretProvider implements SecretProvider {

    public static final String PLUGIN_ID = "mock";

    private final MockSecretProviderConfiguration configuration;

    Map<String, AtomicInteger> errorReturned = new ConcurrentHashMap<>();
    Map<String, AtomicInteger> renewalReturned = new ConcurrentHashMap<>();

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        MockSecretLocation location = MockSecretLocation.fromLocation(secretMount.location());
        log.info("{}-{} resolving secret: {}", PLUGIN_ID, SECRET_PROVIDER, location.secret());

        // return error first
        Optional<ConfiguredError> errorOpt = configuration.getError(location.secret());
        if (errorOpt.isPresent()) {
            ConfiguredError error = errorOpt.get();
            AtomicInteger errorReturned = this.errorReturned.computeIfAbsent(location.secret(), __ -> new AtomicInteger());
            if (error.repeat() == 0 || error.repeat() > 0 && errorReturned.getAndIncrement() < error.repeat()) {
                if (error.repeat() > 0 && secretMount.retryOnError()) {
                    log.info(
                        "{}-{} retrying secret: {} [{}/{}]",
                        PLUGIN_ID,
                        SECRET_PROVIDER,
                        secretMount,
                        error.repeat(),
                        errorReturned.get()
                    );
                    return resolve(secretMount);
                }
                String message = "fake error while getting secret [%s]: %s".formatted(location.secret(), error.message());
                log.info("{}-{} simulating error: {}", PLUGIN_ID, SECRET_PROVIDER, message);
                return Maybe.error(new MockSecretProviderException(message));
            }
        }

        if (renewalReturned.containsKey(location.secret())) {
            AtomicInteger counter = renewalReturned.get(location.secret());
            Renewal renewal = configuration.getConfiguredRenewals().get(location.secret());
            if (counter.get() < renewal.revisions().size()) {
                // next revision and increment
                return Maybe.just(getAndIncrement(renewal, counter));
            } else if (renewal.loop()) {
                // went over just reset
                counter.set(0);
            } else {
                // went over no looping => get latest, simulate that won't change anymore
                return Maybe.just(SecretMap.of(renewal.revisions().get(counter.get() - 1)));
            }
        }
        if (configuration.getConfiguredRenewals().containsKey(location.secret())) {
            renewalReturned.put(location.secret(), new AtomicInteger());
        }

        // standard resolution
        Map<String, Object> secretMap = ConfigHelper.removePrefix(configuration.getSecrets(), location.secret());
        if (secretMap.isEmpty()) {
            log.info("{}-{} no secrets for: {}", PLUGIN_ID, SECRET_PROVIDER, location.secret());
            return Maybe.empty();
        }
        log.info("{}-{} found secrets ({}) for: {}", PLUGIN_ID, SECRET_PROVIDER, secretMap.size(), location.secret());
        return Maybe.just(SecretMap.of(secretMap));
    }

    private static SecretMap getAndIncrement(Renewal renewal, AtomicInteger counter) {
        return SecretMap.of(renewal.revisions().get(counter.getAndIncrement()));
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount) {
        MockSecretLocation location = MockSecretLocation.fromLocation(secretMount.location());
        List<ConfiguredEvent> list = configuration
            .getConfiguredEvents()
            .stream()
            .filter(e -> e.secret().equals(location.secret()))
            .toList();
        return Flowable
            .fromIterable(list)
            .delay(configuration.getWatchesDelayDuration(), configuration.getWatchesDelayUnit())
            .flatMapSingle(event -> {
                if (Objects.equals(event.error(), "null")) {
                    return Single.just(new SecretEvent(event.type(), SecretMap.of(event.data())));
                } else {
                    return Single.error(new MockSecretProviderException(event.error()));
                }
            });
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        if (url.pluginIdMatchURLProvider() && !Objects.equals(PLUGIN_ID, url.provider())) {
            throw new MockSecretProviderException("url does not start with '%s%s'".formatted(SecretURL.SCHEME, PLUGIN_ID));
        }
        return new SecretMount(url.provider(), MockSecretLocation.fromUrl(url), url.key(), url, true);
    }
}
