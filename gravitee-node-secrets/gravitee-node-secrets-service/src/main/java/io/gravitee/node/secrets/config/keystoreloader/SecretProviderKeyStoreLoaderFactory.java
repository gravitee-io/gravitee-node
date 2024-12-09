package io.gravitee.node.secrets.config.keystoreloader;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.secrets.config.GraviteeConfigurationSecretResolver;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecretProviderKeyStoreLoaderFactory implements KeyStoreLoaderFactory<KeyStoreLoaderOptions> {

    private static final List<String> SUPPORTED_TYPES = Arrays.asList(
        KeyStoreLoader.CERTIFICATE_FORMAT_PEM,
        KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12,
        KeyStoreLoader.CERTIFICATE_FORMAT_JKS
    );

    final GraviteeConfigurationSecretResolver secretResolverDispatcher;

    @Override
    public boolean canHandle(KeyStoreLoaderOptions options) {
        final String secretLocation = options.getSecretLocation();
        return (
            secretLocation != null &&
            options.getType() != null &&
            SUPPORTED_TYPES.contains(options.getType().toUpperCase()) &&
            secretResolverDispatcher.canHandle(secretLocation)
        );
    }

    @Override
    public KeyStoreLoader create(KeyStoreLoaderOptions options) {
        return new SecretProviderKeyStoreLoader(secretResolverDispatcher, options);
    }
}
