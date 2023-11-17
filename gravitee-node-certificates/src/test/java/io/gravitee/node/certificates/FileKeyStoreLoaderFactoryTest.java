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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileKeyStoreLoaderFactoryTest {

    private FileKeyStoreLoaderFactory cut;

    @Before
    public void before() {
        cut = new FileKeyStoreLoaderFactory();
    }

    @Test
    public void shouldHandleOptionsWithPKCS12() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .keyStorePath(getPath("localhost.p12"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithPKCS12WithoutPath() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .keyStorePath(null)
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldHandleOptionsWithJKS() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .keyStorePath(getPath("localhost.jks"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithJKSWithoutPath() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .keyStorePath(null)
            .build();

        assertFalse(cut.canHandle(options));
    }

    @Test
    public void shouldHandleOptionsWithPEM() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .keyStorePath(getPath("localhost.jks"))
            .build();

        assertTrue(cut.canHandle(options));
    }

    @Test
    public void shouldNotHandleOptionsWithSelfSigned() {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .keyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
            .keyStorePath(null)
            .build();

        assertFalse(cut.canHandle(options));
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
