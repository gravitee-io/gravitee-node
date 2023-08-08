package io.gravitee.node.plugin.secretprovider.aws.config.auth;

import static io.gravitee.node.plugin.secretprovider.aws.config.auth.AWSAuthConfig.Provider.CHAIN;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 * see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html">Credential Chain</a>
 */
public class AWSChainAuthConfig extends AWSAuthConfig {

    public AWSChainAuthConfig() {
        super(CHAIN);
    }

    @Override
    public AwsCredentialsProvider toAWSCredentials() {
        return DefaultCredentialsProvider.create();
    }
}
