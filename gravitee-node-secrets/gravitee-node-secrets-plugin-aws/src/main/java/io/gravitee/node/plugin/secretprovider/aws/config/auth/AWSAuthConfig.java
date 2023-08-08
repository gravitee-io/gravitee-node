package io.gravitee.node.plugin.secretprovider.aws.config.auth;

import lombok.NoArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
public abstract class AWSAuthConfig {

    protected Provider provider;

    protected AWSAuthConfig(Provider provider) {
        this.provider = provider;
    }

    public abstract AwsCredentialsProvider toAWSCredentials();

    /**
     * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
     * @author GraviteeSource Team
     */
    public enum Provider {
        STATIC,
        CHAIN,
    }
}
