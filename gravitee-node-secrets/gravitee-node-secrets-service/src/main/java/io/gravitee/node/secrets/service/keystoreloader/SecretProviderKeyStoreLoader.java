package io.gravitee.node.secrets.service.keystoreloader;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SecretProviderKeyStoreLoader implements KeyStoreLoader {

    private final List<Consumer<KeyStoreBundle>> listeners = new ArrayList<>();

    private final GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher;
    private final KeyStoreLoaderOptions options;
    private Disposable watch;

    public SecretProviderKeyStoreLoader(
        GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher,
        KeyStoreLoaderOptions options
    ) {
        this.secretResolverDispatcher = secretResolverDispatcher;
        this.options = options;
    }

    @Override
    public void start() {
        final SecretMount secretMount = secretResolverDispatcher.toSecretMount(options.getSecretLocation());
        createBundleAndNotify(secretResolverDispatcher.resolve(secretMount).blockingGet(), secretMount);
        if (options.isWatch()) {
            this.watch =
                secretResolverDispatcher
                    .watch(secretMount)
                    .skip(1) // watch will get the data again, we don't need it
                    .subscribe(secretMap -> createBundleAndNotify(secretMap, secretMount), ex -> log.error("cannot create keystore", ex));
        }
    }

    private void createBundleAndNotify(SecretMap secretMap, SecretMount secretMount) {
        switch (options.getKeyStoreType().toUpperCase()) {
            case KeyStoreLoader.CERTIFICATE_FORMAT_PEM -> notifyListeners(
                new KeyStoreBundle(
                    KeyStoreUtils.initFromPem(
                        secretMap
                            .wellKnown(SecretMap.WellKnownSecretKey.CERTIFICATE)
                            .map(Secret::asString)
                            .orElseThrow(() ->
                                new IllegalArgumentException(
                                    "no pem certificate found in secret. If a ?keymap has been set make sure it contains ?keymap=certificate:<cert key in secret data>)"
                                )
                            ),
                        secretMap
                            .wellKnown(SecretMap.WellKnownSecretKey.PRIVATE_KEY)
                            .map(Secret::asString)
                            .orElseThrow(() ->
                                new IllegalArgumentException(
                                    "no pem private key found in secret. If a ?keymap has been set make sure it contains ?keymap=private_key:<cert key in secret data>)"
                                )
                            ),
                        options.getKeyStorePassword(),
                        options.getDefaultAlias()
                    ),
                    options.getKeyStorePassword(),
                    options.getDefaultAlias()
                )
            );
            case KeyStoreLoader.CERTIFICATE_FORMAT_JKS, KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12 -> notifyListeners(
                new KeyStoreBundle(
                    KeyStoreUtils.initFromContent(
                        options.getKeyStoreType(),
                        secretMap
                            .getSecret(secretMount)
                            .map(Secret::asString)
                            .orElseThrow(() ->
                                new IllegalArgumentException("no keystore value found for key '%s'".formatted(secretMount.key()))
                            ),
                        options.getKeyStorePassword()
                    ),
                    options.getKeyStorePassword(),
                    options.getDefaultAlias()
                )
            );
            default -> log.warn("some ssl related secrets were changes but not handled");
        }
    }

    @Override
    public void stop() {
        if (watch != null) {
            watch.dispose();
        }
    }

    @Override
    public void addListener(Consumer<KeyStoreBundle> listener) {
        listeners.add(listener);
    }

    void notifyListeners(KeyStoreBundle bundle) {
        listeners.forEach(c -> c.accept(bundle));
    }
}
