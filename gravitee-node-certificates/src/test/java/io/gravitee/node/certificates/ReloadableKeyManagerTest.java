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
package io.gravitee.node.certificates;

import static io.gravitee.node.api.certificate.KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12;
import static io.gravitee.node.certificates.ReloadableKeyManager.MAX_SNI_DOMAINS;
import static org.junit.Assert.assertEquals;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ReloadableKeyManagerTest {

    @Mock
    private SSLEngine sslEngine;

    @Mock
    private ExtendedSSLSession sslSession;

    private ReloadableKeyManager cut;

    @Before
    public void before() {
        cut = new ReloadableKeyManager();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotLoadIfAliasUnknown() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("unknown", keyStore, "secret", false);
    }

    @Test
    public void shouldChooseDefaultAliasWhenNoSNI() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("localhost", keyStore, "secret", false);

        // No SNI, default alias is returned.
        assertEquals("localhost", cut.chooseEngineServerAlias("void", null, sslEngine));
    }

    @Test
    public void shouldChooseWildcard() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("localhost", keyStore, "secret", true);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.singletonList(new SNIHostName("test.localhost.com")));

        assertEquals("wildcard", cut.chooseEngineServerAlias("void", null, sslEngine));

        assertTrue(cut.getSniDomainAliases().containsKey("test.localhost.com"));
    }

    @Test
    public void shouldFallbackToDefaultAliasIfUnknown() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("localhost", keyStore, "secret", true);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.singletonList(new SNIHostName("unknown.com")));

        assertEquals("localhost", cut.chooseEngineServerAlias("void", null, sslEngine));
    }

    @Test
    public void shouldFallbackToDefaultAliasIfNoServerName() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("localhost", keyStore, "secret", true);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames()).thenReturn(Collections.emptyList());

        assertEquals("localhost", cut.chooseEngineServerAlias("void", null, sslEngine));
    }

    @Test
    public void shouldStopCacheWhenCacheIsFull() {
        final KeyStore keyStore = KeyStoreUtils.initFromPath(CERTIFICATE_FORMAT_PKCS12, getPath("all-in-one.p12"), "secret");

        cut.load("localhost", keyStore, "secret", true);

        AtomicInteger counter = new AtomicInteger(0);

        when(sslEngine.getHandshakeSession()).thenReturn(sslSession);
        when(sslSession.getRequestedServerNames())
            .thenAnswer(i -> Collections.singletonList(new SNIHostName("unknown" + counter.getAndIncrement() + ".com")));

        // Populate the cache up to max size.
        int numToFake = MAX_SNI_DOMAINS - cut.getSniDomainAliases().size();
        for (int i = 0; i < numToFake; i++) {
            cut.getSniDomainAliases().put("fake" + i, "fake");
        }

        assertEquals(MAX_SNI_DOMAINS, cut.getSniDomainAliases().size());

        // Try a new call
        cut.chooseEngineServerAlias("void", null, sslEngine);

        assertEquals(MAX_SNI_DOMAINS, cut.getSniDomainAliases().size());
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
