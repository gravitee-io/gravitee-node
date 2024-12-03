package io.gravitee.node.secrets.service.keystoreloader;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolver;
import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretMount;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SecretProviderKeyStoreLoader extends AbstractKeyStoreLoader<KeyStoreLoaderOptions> {

    private final GraviteeConfigurationSecretResolver secretResolverDispatcher;
    private Disposable watch;

    public SecretProviderKeyStoreLoader(GraviteeConfigurationSecretResolver secretResolverDispatcher, KeyStoreLoaderOptions options) {
        super(options);
        this.secretResolverDispatcher = secretResolverDispatcher;
    }

    @Override
    public void start() {
        final SecretMount secretMount = secretResolverDispatcher.toSecretMount(options.getSecretLocation());
        if (options.isWatch()) {
            this.watch =
                secretResolverDispatcher
                    .watch(secretMount, SecretEvent.Type.UPDATED)
                    .subscribe(secretMap -> createBundleAndNotify(secretMap, secretMount), ex -> log.error("cannot create keystore", ex));
        } else {
            createBundleAndNotify(
                secretResolverDispatcher
                    .resolve(secretMount)
                    .switchIfEmpty(Maybe.error(new SecretManagerException("secret not found: ".concat(options.getSecretLocation()))))
                    .blockingGet(),
                secretMount
            );
        }
    }

    private void createBundleAndNotify(SecretMap secretMap, SecretMount secretMount) {
        switch (options.getType().toUpperCase()) {
            case KeyStoreLoader.CERTIFICATE_FORMAT_PEM -> {
                String loaderId = id();
                KeyStore keyStore = KeyStoreUtils.initFromPem(
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
                    this.getPassword(),
                    options.getDefaultAlias()
                );
                onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, this.getPassword()));
            }
            case KeyStoreLoader.CERTIFICATE_FORMAT_JKS, KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12 -> {
                String loaderId = id();
                KeyStore keyStore = KeyStoreUtils.initFromContent(
                    options.getType(),
                    secretMap
                        .getSecret(secretMount)
                        .map(Secret::asString)
                        .orElseThrow(() -> new IllegalArgumentException("no keystore value found for key '%s'".formatted(secretMount.key()))
                        ),
                    this.getPassword()
                );
                onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, this.getPassword()));
            }
            default -> log.warn("some ssl related secrets were changes but not handled");
        }
    }

    @Override
    public void stop() {
        if (watch != null) {
            watch.dispose();
        }
    }
}
