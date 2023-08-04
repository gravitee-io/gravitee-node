package io.gravitee.node.secrets.service.keystoreloader;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretProviderKeyStoreLoaderFactory implements KeyStoreLoaderFactory {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
        KeyStoreLoader.CERTIFICATE_FORMAT_PEM,
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12,
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS
    );

    final GraviteeConfigurationSecretResolverDispatcher secretResolverDispatcher;

    @Override
    public boolean canHandle(KeyStoreLoaderOptions options) {
        final String secretLocation = options.getSecretLocation();
        return (
            secretLocation != null &&
            SUPPORTED_TYPES.contains(options.getKeyStoreType().toUpperCase()) &&
            secretResolverDispatcher.canHandle(secretLocation)
        );
    }

    @Override
    public KeyStoreLoader create(KeyStoreLoaderOptions options) {
        return new SecretProviderKeyStoreLoader(secretResolverDispatcher, options);
    }
}
