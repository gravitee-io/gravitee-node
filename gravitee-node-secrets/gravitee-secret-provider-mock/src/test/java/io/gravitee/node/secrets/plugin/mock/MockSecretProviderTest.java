package io.gravitee.node.secrets.plugin.mock;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.SecretProvider;
import io.gravitee.node.api.secrets.model.*;
import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.security.util.InMemoryResource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MockSecretProviderTest {

    private SecretProvider cut;

    @BeforeEach
    void setup() {
        InMemoryResource inMemoryResource = new InMemoryResource(
            """
                enabled: true
                secrets:
                    redis:
                        password: r3d1s
                    ldap:
                        password: 1da9
                    flaky:
                        value: now it works
                    retry-test:
                        value: after several retries it works
                    loop:
                        value: loop 1
                    renewable:
                        value: once
                errors:
                    - secret: flaky
                      message: next attempt it should work
                      repeat: 1
                    - secret: kafka
                      message: that's just ain't working
                    - secret: retry-test
                      message: fatal error
                      repeat: 10
                      delayMs: 200
                renewals:
                  - secret: loop
                    loop: true
                    revisions:
                      - data:
                          value: loop 2
                      - data:
                          value: loop 3
                  - secret: renewable
                    revisions:
                      - data:
                          value: twice and no more
                watches:
                    delay:
                        unit: SECONDS
                        duration: 2
                    events:
                        - secret: apikeys
                          data:
                            partner1: "123"
                            partner2: "456"
                          type: CREATED
                        - secret: apikeys
                          data:
                            partner1: "789"
                            partner2: "101112"
                          type: UPDATED
                        - secret: apikeys
                          data: {}
                          error: odd enough message to be unique
                """
        );
        final YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(inMemoryResource);
        Map conf = new LinkedHashMap<>(yaml.getObject());
        this.cut = new MockSecretProviderFactory().create(new MockSecretProviderConfiguration(conf));
    }

    @Test
    void should_create_mount() {
        SecretMount secretMountRedis = cut.fromURL(SecretURL.from("secret://mock/redis:password"));
        assertThat(secretMountRedis.provider()).isEqualTo("mock");
        assertThat((String) secretMountRedis.location().get("secret")).isEqualTo("redis");
        assertThat(secretMountRedis.key()).isEqualTo("password");
    }

    @Test
    void should_resolve() {
        SecretMount secretMountRedis = cut.fromURL(SecretURL.from("secret://mock/redis:password"));
        cut.resolve(secretMountRedis).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("password", "r3d1s")));

        SecretMount secretMountLdap = cut.fromURL(SecretURL.from("secret://mock/ldap"));
        cut.resolve(secretMountLdap).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("password", "1da9")));
    }

    @Test
    void should_return_empty() {
        SecretMount secretMountEmpty = cut.fromURL(SecretURL.from("secret://mock/empty:password"));
        cut.resolve(secretMountEmpty).test().awaitDone(100, TimeUnit.MILLISECONDS).assertNoErrors().assertComplete();
    }

    @Test
    void should_return_an_error() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/kafka"));
        cut
            .resolve(secretMount)
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertError(err -> err.getMessage().contains("that's just ain't working"));
    }

    @Test
    void should_return_an_error_then_work() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/flaky")).withoutRetries();
        cut
            .resolve(secretMount)
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertError(err -> err.getMessage().contains("next attempt it should work"));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "now it works")));
    }

    @Test
    void should_return_a_secret_after_several_200ms_retries() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/retry-test"));
        cut
            .resolve(secretMount)
            .test()
            .awaitDone(6, TimeUnit.SECONDS)
            .assertValue(SecretMap.of(Map.of("value", "after several retries it works")));
    }

    @Test
    void should_watch_values() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/apikeys"));
        cut
            .watch(secretMount)
            .test()
            .awaitDone(3, TimeUnit.SECONDS)
            .assertValueAt(
                0,
                secretEvent ->
                    secretEvent.type() == SecretEvent.Type.CREATED &&
                    secretEvent.secretMap().asMap().values().containsAll(List.of(new Secret("123", false), new Secret("456", false)))
            )
            .assertValueAt(
                1,
                secretEvent ->
                    secretEvent.type() == SecretEvent.Type.UPDATED &&
                    secretEvent.secretMap().asMap().values().containsAll(List.of(new Secret("789", false), new Secret("101112", false)))
            )
            .assertError(err -> err.getMessage().contains("odd enough message to be unique"));
    }

    @Test
    void should_renew_once() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/renewable"));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "once")));
        cut
            .resolve(secretMount)
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("value", "twice and no more")));
        cut
            .resolve(secretMount)
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("value", "twice and no more")));
    }

    @Test
    void should_renew_in_loop() {
        SecretMount secretMount = cut.fromURL(SecretURL.from("secret://mock/loop"));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 2")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 3")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 2")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 3")));
        cut.resolve(secretMount).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
    }
}
