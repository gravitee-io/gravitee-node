package io.gravitee.node.secrets.service.keystoreloader;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.model.SecretMap;
import io.gravitee.node.api.secrets.model.SecretMount;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class SecretProviderKeyStoreLoader extends AbstractKeyStoreLoader<KeyStoreLoaderOptions> {

    private final GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher;
    private Disposable watch;

    public SecretProviderKeyStoreLoader(
        GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher,
        KeyStoreLoaderOptions options
    ) {
        super(options);
        this.secretResolverDispatcher = secretResolverDispatcher;
    }

    @Override
    public void start() {
        final SecretMount secretMount = secretResolverDispatcher.toSecretMount(options.getSecretLocation());
        createBundleAndNotify(secretResolverDispatcher.resolve(secretMount).blockingGet(), secretMount);
        if (options.isWatch()) {
            this.watch =
                secretResolverDispatcher
                    .watch(secretMount, SecretEvent.Type.UPDATED)
                    .subscribe(secretMap -> createBundleAndNotify(secretMap, secretMount), ex -> log.error("cannot create keystore", ex));
        }
    }

    private void createBundleAndNotify(SecretMap secretMap, SecretMount secretMount) {
        switch (options.getType().toUpperCase()) {
            case KeyStoreLoader.CERTIFICATE_FORMAT_PEM -> onEvent(
                KeyStoreEvent.loadEvent(
                    id(),
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
                        this.getPassword(),
                        options.getDefaultAlias()
                    ),
                    this.getPassword(),
                    options.getDefaultAlias()
                )
            );
            case KeyStoreLoader.CERTIFICATE_FORMAT_JKS, KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12 -> onEvent(
                KeyStoreEvent.loadEvent(
                    id(),
                    KeyStoreUtils.initFromContent(
                        options.getType(),
                        secretMap
                            .getSecret(secretMount)
                            .map(Secret::asString)
                            .orElseThrow(() ->
                                new IllegalArgumentException("no keystore value found for key '%s'".formatted(secretMount.key()))
                            ),
                        this.getPassword()
                    ),
                    this.getPassword(),
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
}
