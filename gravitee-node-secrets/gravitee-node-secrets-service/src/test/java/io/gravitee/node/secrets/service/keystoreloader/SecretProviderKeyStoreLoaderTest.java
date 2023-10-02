package io.gravitee.node.secrets.service.keystoreloader;

import static io.gravitee.node.secrets.service.test.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPluginManager;
import io.gravitee.node.secrets.service.conf.GraviteeConfigurationSecretResolverDispatcher;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SecretProviderKeyStoreLoaderTest {

    static final String CERT =
        """
                -----BEGIN CERTIFICATE-----
                MIIDazCCAlOgAwIBAgIUJjfny3beplZzojjkJ1fhbV1RHD4wDQYJKoZIhvcNAQEL
                BQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoM
                GEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMzA5MDgxMDM3MzVaFw0yNDA4
                MjkxMDM3MzVaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEw
                HwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEB
                AQUAA4IBDwAwggEKAoIBAQCt3A4qP+9Rl7iv/wx3fi33sVECYJBTpUMouDl9Amu2
                Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb3ezdwcbFbT8m7Qvec0jId0XhU40m
                b0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAEdRvwfdJJdQ+v75tO2SzuiK460dFo
                rOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM6wI8jnpXoHgN8RJ/2G7SZPyn1rmY
                lEjoX57daAVEtR011nHO97zdncBjfR/iswsfmkhCisbKi5P+Lng9OS3RF5dl30wG
                8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwAps1gfUf1AgMBAAGjUzBRMB0GA1Ud
                DgQWBBQ3syOvxPbQq4GaYFTjP7EantnBzzAfBgNVHSMEGDAWgBQ3syOvxPbQq4Ga
                YFTjP7EantnBzzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQB1
                ws1gimBdXMJ00IgrzyZd6nS9roGAbIueWEnnfVsJAuz1kc1WtGzZPDPW7qUHoZNy
                Lcb/xksIsw8MnFhmC++aiB4c+VmNNeqdY+pHVFhgEuCsH/Mm/Obkvw1zImfOmurp
                QZXEdTZ6uQVYPYZ8kyfABJg5bkCWKc++XbtsFQy2H4Xk8tYvABLKrxh3mkkgTypx
                dxDgjT806ZVjxgXdcryMskFX8amsofowzDwU6u8Wo+SW8jloItWv+j5hCR8eiIIz
                29AxHtIJmaiTidz2eHsjfuhSqKgS74ndeJnsdz5ZHRsWoEtu0t/nIrwSclZKrjBq
                VXwOSZSQT3z99f/MsavL
                -----END CERTIFICATE-----
                """;

    static final String KEY =
        """
                -----BEGIN PRIVATE KEY-----
                MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCt3A4qP+9Rl7iv
                /wx3fi33sVECYJBTpUMouDl9Amu2Gi/W5nsbRQY26KenWPr05wrnDlDvsnLxRXbb
                3ezdwcbFbT8m7Qvec0jId0XhU40mb0DUjCs4vQCyAKde/VpJC0soNsc0Wfx9NWAE
                dRvwfdJJdQ+v75tO2SzuiK460dForOtwVwLKL3KOD0syifUHEKeDJS6eN3h/N1nM
                6wI8jnpXoHgN8RJ/2G7SZPyn1rmYlEjoX57daAVEtR011nHO97zdncBjfR/iswsf
                mkhCisbKi5P+Lng9OS3RF5dl30wG8tiHIOAn2z0eAQNoyr70oLtCaHjC+SPPuzwA
                ps1gfUf1AgMBAAECggEADhqWaZYDL47L1DcwBzeMuhW/2R4FR0vWTWTYgQwjucOZ
                Eulinj00ulqYUyqUPS7LAyB1r2Q+D9WPRVnU/85a9iQdJea/+j1G78BBQny5LB+F
                VljCntkyR75m1X1fCCLq52m+MkCEi5G7ZtErQZCrcPsWmTKqWjSjAPzEiZAA2Wlf
                Z3hemgge3pmASz964TR4Nd1yC6rceEJvAr5d/Ez6MU8mgez9o/ZuaIoi0q4n12NZ
                /rexM9B8rnP93nedNjyy1lCc9+T8x0s7haN/ZjKR3nGj+cp6PCxAgNX18G5shmqR
                6bJrjn0Mu04w2n3bfoG0NNNpf3j06vIP1HNyAuhKlwKBgQDhtzfet3/h68eDJO3m
                oD3oI45vDvesHgIeXPR+BZGsujW6ab1DSUEeZhAgbxooD/NioWZVer/jehgcvJdg
                TUALq63so4Q24DFJp6WdQPU0uLvlajqhykF0SccdFo8iN3xGGbCK8Kb2tHexULaN
                rvPCLZTEjlpPzULUemc70yAVowKBgQDFL7TwMakwiTk4ed26uoru1cth+IOQz1YP
                DoiGvBTU0uvegGCclWxFwkfXfMzqQGpTK2v9EG2afL5CZUnGCSAO2Zq6nTuXpLr4
                GmtosQcJmzA7BDiY86eLDsSCxAQb/5xOqjDIvJR/BZnH7+8duqCWcMqiwYoUdz1n
                qxJCZb6VhwKBgBwI8buL9ypMar9zOslGZeoLYImSxlhucbzrtsJgVrOpfTrmH0fY
                NWpdKuucYRdQw94gReGgGW1boNsQ4Yxoi+fnLvcRaD6YogaP+BYMF2iw+UWJaDbo
                NDEJaN3IC4codRsP3cmkEljaGXPAnqwCauxXVP8E31rCF+bkPSZFFtsZAoGAV1CU
                sneLD67z44ozIOhRdQi+kpdUyt7EoM4yrlbCcqsjPtdh8HRKCWnKHiVpJ6F2c3Wa
                z+hiYDI0nXn0fPi1dV3uIgxVwwRytkIcpbMeBqbtaHSqCzB5VB4p7i2WFD/PmxXJ
                nFnE96onOl2IaIWnbnZrhD5nQkC6tBkQcM5U4ikCgYAUMBYsZJpTnPYojMp6EM9B
                icwZQsuhNFgn+WM2/itFlPH7N/s1cScs4stkS1OzrlzZHLAzOfbqLeTbpNfQM5lE
                utWjVUNvzathT7PMDCxR1VtuNvpAZon5/ResDgimGyr/YvZ5XuriHdudTeAN75TZ
                0LCyEgd6Noz/STJZdPuW+A==
                -----END PRIVATE KEY-----
                """;

    static final String JKS_KEY_STORE =
        "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" +
        "STxACAicQ";

    final List<KeyStoreBundle> keyStores = new ArrayList<>();
    SecretProviderKeyStoreLoaderFactory factory;
    SecretProviderKeyStoreLoader cut;

    @BeforeEach
    void before() {
        DefaultSecretProviderPluginManager pluginManager = newPluginManager();
        MockEnvironment env = newEnvironment();
        env.setProperty("secrets.test.secrets.tlscert", CERT);
        env.setProperty("secrets.test.secrets.tlskey", KEY);
        env.setProperty("secrets.test.secrets.jkskeystore", JKS_KEY_STORE);
        GraviteeConfigurationSecretResolverDispatcher dispatcher = newDispatcher(pluginManager, env);
        this.factory = new SecretProviderKeyStoreLoaderFactory(dispatcher);
    }

    @AfterEach
    void after() {
        if (this.cut != null) {
            this.cut.stop();
        }
    }

    @Test
    void should_resolve_pem_pair_and_notify_of_bundle() {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType("pem")
            .withWatch(false)
            .withSecretLocation("secret://test/test?keymap=certificate:tlscert&keymap=private_key:tlskey")
            .build();
        this.cut = (SecretProviderKeyStoreLoader) factory.create(options);
        this.cut.addListener(keyStores::add);
        this.cut.start();

        // wait to make sure only one secret is fetched
        await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() -> assertThat(keyStores).hasSize(1));
    }

    @Test
    void should_resolve_keystore_and_notify_of_bundle() {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withWatch(false)
            .withKeyStoreType("jks")
            .withKeyStorePassword("123456")
            .withSecretLocation("secret://test/test:jkskeystore")
            .build();
        this.cut = (SecretProviderKeyStoreLoader) factory.create(options);
        this.cut.addListener(keyStores::add);
        this.cut.start();

        // wait to make sure only one secret is fetched
        await().pollDelay(Duration.ofSeconds(1)).untilAsserted(() -> assertThat(keyStores).hasSize(1));
    }

    @Test
    void should_watch_pem_pair_and_notify_of_bundle() {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType("pem")
            .withSecretLocation("secret://test/test?keymap=certificate:tlscert&keymap=private_key:tlskey")
            .build();
        this.cut = (SecretProviderKeyStoreLoader) factory.create(options);
        this.cut.addListener(keyStores::add);
        this.cut.start();
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(keyStores).hasSize(2));
    }

    @Test
    void should_watch_keystore_and_notify_of_bundle() {
        KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType("jks")
            .withKeyStorePassword("123456")
            .withSecretLocation("secret://test/test:jkskeystore")
            .build();
        this.cut = (SecretProviderKeyStoreLoader) factory.create(options);
        this.cut.addListener(keyStores::add);
        this.cut.start();
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(keyStores).hasSize(2));
    }
}
