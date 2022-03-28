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

import static org.junit.Assert.*;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SelfSignedKeyStoreLoaderFactoryTest {

    private SelfSignedKeyStoreLoaderFactory cut;

    @Before
    public void before() {
        cut = new SelfSignedKeyStoreLoaderFactory();
    }

    @Test
    public void shouldHandleOptionsWithSelfSigned() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
            .withKeyStorePath(null)
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithPKCS12() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithJKS() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithPEM() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .build();

        assertFalse(cut.canHandle(options));
    }
}
