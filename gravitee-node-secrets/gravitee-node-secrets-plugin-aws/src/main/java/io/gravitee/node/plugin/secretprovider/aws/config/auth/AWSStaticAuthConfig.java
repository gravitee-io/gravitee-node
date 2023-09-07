package io.gravitee.node.plugin.secretprovider.aws.config.auth;

import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.Map;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@Data
@FieldNameConstants
public class AWSStaticAuthConfig extends AWSAuthConfig {

    private String accessKeyId;
    private String secretAccessKey;

    public AWSStaticAuthConfig(Map<String, Object> properties) {
        super(Provider.STATIC);
        this.accessKeyId = ConfigHelper.getStringOrSecret(properties, Fields.accessKeyId);
        this.secretAccessKey = ConfigHelper.getStringOrSecret(properties, Fields.secretAccessKey);
    }

    @Override
    public AwsCredentialsProvider toAWSCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }
}
