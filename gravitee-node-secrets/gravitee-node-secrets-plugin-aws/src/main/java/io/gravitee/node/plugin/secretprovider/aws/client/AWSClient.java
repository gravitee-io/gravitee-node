package io.gravitee.node.plugin.secretprovider.aws.client;

import io.gravitee.node.plugin.secretprovider.aws.config.AWSConfig;
import io.gravitee.node.plugin.secretprovider.aws.config.AWSSecretLocation;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.vertx.core.json.Json;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AWSClient {

    private final SecretsManagerClient client;

    public AWSClient(AWSConfig config) {
        SecretsManagerClientBuilder secretsManagerClientBuilder = SecretsManagerClient
            .builder()
            .credentialsProvider(config.credentialsProvider())
            .httpClient(
                UrlConnectionHttpClient.builder().connectionTimeout(Duration.of(config.getConnectionTimeoutMs(), ChronoUnit.MILLIS)).build()
            )
            .region(config.getAWSRegion())
            .fipsEnabled(config.isFipsEnabled());
        if (config.getEndpointOverrideURI() != null) {
            secretsManagerClientBuilder.endpointOverride(config.getEndpointOverrideURI());
        }
        this.client = secretsManagerClientBuilder.build();
    }

    public Map<String, Object> getSecretValue(AWSSecretLocation secretLocation) {
        final String json;
        try {
            GetSecretValueRequest.Builder builder = GetSecretValueRequest.builder().secretId(secretLocation.secretName());
            GetSecretValueResponse secretValue = client.getSecretValue(builder.build());
            json = secretValue.secretString();
            if (json == null || json.isEmpty()) {
                throw new SecretManagerException("AWS secret binary format is not supported");
            }
        } catch (Exception e) {
            throw new SecretManagerException(e);
        }

        try {
            return Json.decodeValue(json, Map.class);
        } catch (Exception e) {
            throw new SecretManagerException("The AWS secret payload is required to a JSON flat object", e);
        }
    }

    public void stop() {
        if (this.client != null) {
            this.client.close();
        }
    }
}
