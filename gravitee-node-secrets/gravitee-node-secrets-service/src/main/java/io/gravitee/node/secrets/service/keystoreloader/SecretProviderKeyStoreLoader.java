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
import io.gravitee.secrets.api.core.SecretURL;
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
        final SecretURL secretURL = secretResolverDispatcher.asSecretURL(options.getSecretLocation());
        int skip = 0;
        if (options.isWatch()) {
            if (
                // resolve before by default
                !secretURL.queryParamExists(SecretURL.WellKnownQueryParam.RESOLVE_BEFORE_WATCH) ||
                secretURL.queryParamEqualsIgnoreCase(SecretURL.WellKnownQueryParam.RESOLVE_BEFORE_WATCH, "true")
            ) {
                resolveAndNotify(secretURL);
                skip = 1;
            }
            this.watch =
                secretResolverDispatcher
                    .watch(secretURL, SecretEvent.Type.CREATED, SecretEvent.Type.UPDATED)
                    .skip(skip)
                    .subscribe(secretMap -> createBundleAndNotify(secretMap, secretURL), ex -> log.error("cannot create keystore", ex));
        } else {
            resolveAndNotify(secretURL);
        }
    }

    private void resolveAndNotify(SecretURL secretURL) {
        createBundleAndNotify(
            secretResolverDispatcher
                .resolve(secretURL)
                .switchIfEmpty(Maybe.error(new SecretManagerException("secret not found: ".concat(options.getSecretLocation()))))
                .blockingGet(),
            secretURL
        );
    }

    private void createBundleAndNotify(SecretMap secretMap, SecretURL secretURL) {
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
                        .getSecret(secretURL)
                        .map(Secret::asString)
                        .orElseThrow(() -> new IllegalArgumentException("no keystore value found for key '%s'".formatted(secretURL.key()))),
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
