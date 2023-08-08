package io.gravitee.node.plugin.secretprovider.aws;

import io.gravitee.node.plugin.secretprovider.aws.client.AWSClient;
import io.gravitee.node.plugin.secretprovider.aws.config.AWSConfig;
import io.gravitee.node.plugin.secretprovider.aws.config.AWSSecretLocation;
import io.gravitee.node.secrets.api.SecretProvider;
import io.gravitee.node.secrets.api.model.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AWSSecretProvider implements SecretProvider {

    private final AWSClient client;

    public AWSSecretProvider(AWSConfig config) {
        this.client = new AWSClient(config);
    }

    @Override
    public Maybe<SecretMap> resolve(SecretMount secretMount) {
        AWSSecretLocation awsSecretLocation = AWSSecretLocation.fromLocation(secretMount.location());
        try {
            Map<String, Object> secretValue = client.getSecretValue(awsSecretLocation);
            SecretMap secretMap = new SecretMap(
                secretValue.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new Secret(e.getValue())))
            );
            return Maybe.just(secretMap);
        } catch (Exception e) {
            return Maybe.error(e);
        }
    }

    @Override
    public Flowable<SecretEvent> watch(SecretMount secretMount, SecretEvent.Type... events) {
        // Would require further investigation to use event bridge
        // in that case customer must set a queue for those event.
        // and listen to it via SNS for instance.
        // see https://docs.aws.amazon.com/secretsmanager/latest/userguide/monitoring-eventbridge.html
        // we could also setup a polling on rotation schedule ?rotation=12d and poll every 12 days.
        return Flowable.error(new NoSuchMethodException("not watch implemented"));
    }

    @Override
    public SecretMount fromURL(SecretURL url) {
        AWSSecretLocation awsSecretLocation = AWSSecretLocation.fromURL(url);
        return new SecretMount(url.provider(), new SecretLocation(awsSecretLocation.asMap()), awsSecretLocation.key(), url);
    }

    @Override
    public AWSSecretProvider stop() {
        client.stop();
        return this;
    }
}
