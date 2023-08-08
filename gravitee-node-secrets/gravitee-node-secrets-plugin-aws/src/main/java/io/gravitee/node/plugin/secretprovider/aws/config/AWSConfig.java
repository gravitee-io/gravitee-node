package io.gravitee.node.plugin.secretprovider.aws.config;

import io.gravitee.node.plugin.secretprovider.aws.config.auth.AWSAuthConfig;
import io.gravitee.node.plugin.secretprovider.aws.config.auth.AWSChainAuthConfig;
import io.gravitee.node.plugin.secretprovider.aws.config.auth.AWSStaticAuthConfig;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.util.ConfigHelper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@FieldNameConstants
public class AWSConfig implements SecretManagerConfiguration {

    private boolean enabled;
    private String region;
    private AWSAuthConfig authConfig;
    private boolean fipsEnabled;
    private String endpointOverride;

    @Setter(AccessLevel.PRIVATE)
    private URI endpointOverrideURI;

    private int connectionTimeoutMs;

    public AWSConfig(Map<String, Object> properties) {
        Objects.requireNonNull(properties);
        enabled = (boolean) Objects.requireNonNull(properties.get(Fields.enabled));
        if (!isEnabled()) {
            return;
        }
        region = (String) Objects.requireNonNull(properties.get(Fields.region));
        fipsEnabled = (boolean) properties.getOrDefault(Fields.fipsEnabled, false);
        endpointOverride = (String) properties.get(Fields.endpointOverride);
        connectionTimeoutMs = (int) properties.getOrDefault(Fields.connectionTimeoutMs, 5000);
        if (endpointOverride != null && !endpointOverride.isBlank()) {
            try {
                endpointOverrideURI = new URI(endpointOverride);
            } catch (URISyntaxException e) {
                throw new SecretManagerException(e);
            }
        }
        AWSAuthConfig.Provider provider = ConfigHelper.enumValueOfIgnoreCase(
            (String) properties.get("auth.provider"),
            AWSAuthConfig.Provider.class,
            "auth.provider"
        );
        if (provider == AWSAuthConfig.Provider.STATIC) {
            authConfig = new AWSStaticAuthConfig(ConfigHelper.removePrefix(properties, "auth.config"));
        } else if (provider == AWSAuthConfig.Provider.CHAIN) {
            authConfig = new AWSChainAuthConfig();
        }
    }

    public Region getAWSRegion() {
        return Region.of(region);
    }

    public AwsCredentialsProvider credentialsProvider() {
        return authConfig.toAWSCredentials();
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
