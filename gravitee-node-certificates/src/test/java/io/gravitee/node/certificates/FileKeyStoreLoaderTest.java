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

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileKeyStoreLoaderTest {

    private FileKeyStoreLoader cut;

    @After
    public void after() {
        cut.stop();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotStartOnMissingFile() {
        // Make sure an exception is thrown in case of missing file.
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKeyStorePath("/path-to-unknown.p12")
            .withKeyStorePassword("secret")
            .withDefaultAlias("localhost")
            .withWatch(false)
            .build();

        cut = new FileKeyStoreLoader(options);
        cut.start();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotStartOnInvalidKeyStore() throws IOException {
        // Make sure an exception is thrown in case of invalid keystore.
        final File tempKeyStore = File.createTempFile("gio", ".p12");
        FileCopyUtils.copy(new byte[0], tempKeyStore);

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKeyStorePath(tempKeyStore.getAbsolutePath())
            .withKeyStorePassword("secret")
            .withDefaultAlias("localhost")
            .withWatch(false)
            .build();

        cut = new FileKeyStoreLoader(options);
        cut.start();
    }

    @Test
    public void shouldLoadPKCS12() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .withKeyStorePath(getPath("all-in-one.p12"))
            .withKeyStorePassword("secret")
            .withDefaultAlias("localhost")
            .withWatch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);

        cut.start();

        assertNotNull(bundleRef.get());

        final KeyStoreBundle keyStoreBundle = bundleRef.get();
        assertNotNull(keyStoreBundle);
        assertNotNull(keyStoreBundle.getKeyStore());
        assertEquals(4, keyStoreBundle.getKeyStore().size());
        assertEquals("localhost", keyStoreBundle.getDefaultAlias());
        assertEquals("secret", keyStoreBundle.getPassword());
    }

    @Test
    public void shouldLoadJKS() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .withKeyStorePath(getPath("all-in-one.jks"))
            .withKeyStorePassword("secret")
            .withDefaultAlias("localhost")
            .withWatch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);

        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();
        assertNotNull(keyStoreBundle);
        assertNotNull(keyStoreBundle.getKeyStore());
        assertEquals(4, keyStoreBundle.getKeyStore().size());
        assertEquals("localhost", keyStoreBundle.getDefaultAlias());
        assertEquals("secret", keyStoreBundle.getPassword());
    }

    @Test
    public void shouldLoadPEMs() throws KeyStoreException {
        final ArrayList<CertificateOptions> certificates = new ArrayList<>();

        certificates.add(new CertificateOptions(getPath("localhost.cer"), getPath("localhost.key")));
        certificates.add(new CertificateOptions(getPath("localhost2.cer"), getPath("localhost2.key")));
        certificates.add(new CertificateOptions(getPath("localhost3.cer"), getPath("localhost3.key")));
        certificates.add(new CertificateOptions(getPath("wildcard.cer"), getPath("wildcard.key")));

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .withKeyStoreCertificates(certificates)
            .withKeyStorePassword("secret")
            .withDefaultAlias("localhost")
            .withWatch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);

        cut.start();

        final KeyStoreBundle keyStoreBundle = bundleRef.get();
        assertNotNull(keyStoreBundle);
        assertNotNull(keyStoreBundle.getKeyStore());
        assertEquals(4, keyStoreBundle.getKeyStore().size());
        assertNull(keyStoreBundle.getDefaultAlias()); // Alias can't be defined for pem.
        assertEquals("secret", keyStoreBundle.getPassword());
    }

    @Test
    public void shouldWatchFileAndDetectChanges() throws IOException, InterruptedException {
        final File tempKeyStore = File.createTempFile("gio", ".p12");
        FileCopyUtils.copy(new File(getPath("localhost.p12")), tempKeyStore);

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .withKeyStorePath(tempKeyStore.getAbsolutePath())
            .withKeyStorePassword("secret")
            .withWatch(true)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(null);
        cut.addListener(bundleRef::set);

        cut.start();

        KeyStoreBundle bundle = bundleRef.get();

        // Check the initial keystore has been properly loaded.
        assertNotNull(bundle);
        assertEquals("localhost", KeyStoreUtils.getDefaultAlias(bundle.getKeyStore()));

        // Register a listener to trap the reload event.
        CountDownLatch latch = new CountDownLatch(1);
        cut.addListener(kb -> latch.countDown());

        long started = System.currentTimeMillis();

        // Wait the watcher to be started started before making changes on the file keystore.
        while (!cut.isWatching()) {
            Thread.sleep(50);
            if (System.currentTimeMillis() - started > 10000) {
                fail("Watcher time to start exceed 10s");
            }
        }

        FileCopyUtils.copy(new File(getPath("localhost2.p12")), tempKeyStore);

        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));

        bundle = bundleRef.get();

        assertNotNull(bundle);
        assertEquals("localhost2", KeyStoreUtils.getDefaultAlias(bundle.getKeyStore()));

        cut.stop();
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }
}
