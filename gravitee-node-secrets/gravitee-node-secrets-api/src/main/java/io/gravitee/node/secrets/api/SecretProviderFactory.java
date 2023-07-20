package io.gravitee.node.secrets.api;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderFactory<T extends SecretManagerConfiguration> {
    SecretProvider create(T configuration);
}
