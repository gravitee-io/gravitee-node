package io.gravitee.node.secrets.plugin.mock;

import io.gravitee.node.secrets.plugin.mock.conf.MockSecretProviderConfiguration;
import io.gravitee.secrets.api.core.Secret;
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
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

    private MockSecretProvider cut;

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
        this.cut =
            (MockSecretProvider) new MockSecretProviderFactory()
                .create(new MockSecretProviderConfiguration((Map) new LinkedHashMap<>(yaml.getObject())));
    }

    @Test
    void should_resolve() {
        cut
            .resolve(SecretURL.from("secret://mock/redis:password"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("password", "r3d1s")));

        cut
            .resolve(SecretURL.from("secret://mock/ldap"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("password", "1da9")));
    }

    @Test
    void should_return_empty() {
        cut
            .resolve(SecretURL.from("secret://mock/empty:password"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    void should_return_an_error() {
        cut
            .resolve(SecretURL.from("secret://mock/kafka"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertError(err -> err.getMessage().contains("that's just ain't working"));
    }

    @Test
    void should_return_an_error_then_work() {
        SecretURL url = SecretURL.from("secret://mock/flaky");

        cut
            .resolve(url)
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertError(err -> err.getMessage().contains("next attempt it should work"));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "now it works")));
    }

    @Test
    void should_watch_values() {
        cut
            .watch(SecretURL.from("secret://mock/apikeys"))
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
        SecretURL url = SecretURL.from("secret://mock/renewable");

        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "once")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "twice and no more")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "twice and no more")));
    }

    @Test
    void should_renew_in_loop() {
        SecretURL url = SecretURL.from("secret://mock/loop");
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 2")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 3")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 2")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 3")));
        cut.resolve(url).test().awaitDone(100, TimeUnit.MILLISECONDS).assertValue(SecretMap.of(Map.of("value", "loop 1")));
    }

    @Test
    void should_updates_secret() {
        cut
            .resolve(SecretURL.from("secret://mock/redis:password"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("password", "r3d1s")));
        cut.updateSecret("redis", "username", "teddy");
        cut
            .resolve(SecretURL.from("secret://mock/redis:password"))
            .test()
            .awaitDone(100, TimeUnit.MILLISECONDS)
            .assertValue(SecretMap.of(Map.of("password", "r3d1s", "username", "teddy")));
    }
}
