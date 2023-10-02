package io.gravitee.node.api.secrets;

/**
 * This is the plugin class for  plugins of type "secret-provider".
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretProviderFactory<T extends SecretManagerConfiguration> {
    /**
     * Creates a new instance of {@link SecretProvider}
     *
     * @param configuration the configuration object
     * @return a secret provider if the configuration can be consumed.
     */
    SecretProvider create(T configuration);
}
