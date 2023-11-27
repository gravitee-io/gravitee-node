/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.certificates.x509;

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12;
import static io.gravitee.node.certificates.x509.RefreshableX509KeyManagerDelegator.MAX_SNI_DOMAINS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.common.util.KeyStoreUtils;
import java.security.KeyStore;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@RunWith(MockitoJUnitRunner.class)
public class X509KeyManagerDelegatorTest {

    public static final String PASSWORD = "secret";

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private ExtendedSSLSession sslSession;

    private RefreshableX509KeyManagerDelegator cut;

    @Before
    public void before() {
        cut = new RefreshableX509KeyManagerDelegator("http", true);
    }

    @Test
    public void should_choose_default_alias_when_no_sni() {
        final KeyStore keyStore = loadAllInOne();
        cut = new RefreshableX509KeyManagerDelegator("http", false);
        cut.refresh(keyStore, PASSWORD.toCharArray(), "localhost");

        // No SNI, default alias is returned.
        assertThat(cut.chooseEngineServerAlias("void", null, sslEngine)).isEqualTo("localhost");
    }

    @Test
    public void should_choose_wildcard() {
        final KeyStore keyStore = loadAllInOne();

        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.singletonList(new SNIHostName("test.localhost.com")));

        assertThat(cut.chooseEngineServerAlias("void", null, sslEngine)).isEqualTo("wildcard");
        assertTrue(cut.getSniDomainAliases().containsKey("test.localhost.com"));
    }

    @Test
    public void should_fallback_to_default_alias_if_unknown() {
        final KeyStore keyStore = loadAllInOne();

        cut.refresh(keyStore, PASSWORD.toCharArray(), "localhost");

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.singletonList(new SNIHostName("unknown.com")));

        assertThat(cut.chooseEngineServerAlias("void", null, sslEngine)).isEqualTo("localhost");
    }

    @Test
    public void should_fallback_to_default_alias_if_no_server_name() {
        final KeyStore keyStore = loadAllInOne();

        cut.refresh(keyStore, PASSWORD.toCharArray(), "localhost");

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.emptyList());

        assertThat(cut.chooseEngineServerAlias("void", null, sslEngine)).isEqualTo("localhost");
    }

    @Test
    public void should_get_private_key() {
        final KeyStore keyStore = loadAllInOne();
        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        assertThat(cut.getPrivateKey("localhost")).isNotNull();
    }

    @Test
    public void should_get_null_private_key_when_alias_is_unknown() {
        final KeyStore keyStore = loadAllInOne();
        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        assertThat(cut.getPrivateKey("unknown")).isNull();
    }

    @Test
    public void should_get_certificate_chain() {
        final KeyStore keyStore = loadAllInOne();
        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        assertThat(cut.getCertificateChain("localhost")).isNotNull();
    }

    @Test
    public void should_get_null_certificate_chain_when_alias_is_unknown() {
        final KeyStore keyStore = loadAllInOne();
        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        assertThat(cut.getCertificateChain(null)).isNull();
    }

    @Test
    public void should_stop_cache_when_cache_is_full() {
        final KeyStore keyStore = loadAllInOne();

        cut.refresh(keyStore, PASSWORD.toCharArray(), null);

        AtomicInteger counter = new AtomicInteger(0);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames())
            .thenAnswer(i -> Collections.singletonList(new SNIHostName("unknown" + counter.getAndIncrement() + ".com")));

        // Populate the cache up to max size.
        int numToFake = MAX_SNI_DOMAINS - cut.getSniDomainAliases().size();
        for (int i = 0; i < numToFake; i++) {
            cut.getSniDomainAliases().put("fake" + i, "fake");
        }

        assertThat(cut.getSniDomainAliases()).hasSize(MAX_SNI_DOMAINS);

        // Try a new call
        cut.chooseEngineServerAlias("void", null, sslEngine);

        assertThat(cut.getSniDomainAliases()).hasSize(MAX_SNI_DOMAINS);
    }

    private KeyStore loadAllInOne() {
        return KeyStoreUtils.initFromPath(
            CERTIFICATE_FORMAT_PKCS12,
            this.getClass().getResource("/keystores/all-in-one.p12").getPath(),
            PASSWORD
        );
    }
}
