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
package io.gravitee.node.certificates.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Fail.fail;
import static org.awaitility.Awaitility.await;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.util.FileCopyUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileKeyStoreLoaderTest {

    private FileKeyStoreLoader cut;

    @AfterEach
    public void after() {
        cut.stop();
    }

    @Test
    void should_not_start_on_missing_file() {
        // Make sure an exception is thrown in case of missing file.
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of("/path-to-unknown.p12"))
            .password("secret")
            .defaultAlias("localhost")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);
        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_start_on_invalid_key_store() throws IOException {
        // Make sure an exception is thrown in case of invalid keystore.
        final File tempKeyStore = File.createTempFile("gio", ".p12");
        FileCopyUtils.copy(new byte[0], tempKeyStore);

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(tempKeyStore.getAbsolutePath()))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);
        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_load_pkcs12() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(getPath("all-in-one.p12")))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        assertThat(eventRef.get()).isNotNull();

        final KeyStoreEvent event = eventRef.get();
        assertThat(event).isNotNull();
        assertThat(event.loaderId()).isEqualTo(cut.id());
        KeyStoreEvent.LoadEvent loadEvent = (KeyStoreEvent.LoadEvent) event;
        assertThat(loadEvent.keyStore()).isNotNull();
        assertThat(loadEvent.keyStore().size()).isEqualTo(4);
        assertThat(loadEvent.password()).isEqualTo("secret");
    }

    @Test
    void should_load_pkcs12_with_mix_of_private_keys_and_trusted_entries() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(getPath("mixed-entries.p12")))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        assertThat(eventRef.get()).isNotNull();

        final KeyStoreEvent event = eventRef.get();
        assertThat(event).isNotNull();
        assertThat(event.loaderId()).isEqualTo(cut.id());
        KeyStoreEvent.LoadEvent loadEvent = (KeyStoreEvent.LoadEvent) event;
        assertThat(loadEvent.keyStore()).isNotNull();
        assertThat(loadEvent.keyStore().size()).isEqualTo(6);
        assertThat(loadEvent.password()).isEqualTo("secret");
    }

    @Test
    void should_load_jks() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS)
            .paths(getPaths("all-in-one.jks"))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        final KeyStoreEvent keyStoreEvent = eventRef.get();
        assertThat(keyStoreEvent).isNotNull();
        assertThat(keyStoreEvent.loaderId()).isEqualTo(cut.id());
        KeyStoreEvent.LoadEvent loadEvent = (KeyStoreEvent.LoadEvent) keyStoreEvent;
        assertThat(loadEvent.keyStore()).isNotNull();
        assertThat(loadEvent.keyStore().size()).isEqualTo(4);
        assertThat(loadEvent.password()).isEqualTo("secret");
    }

    @Test
    void should_load_pems() throws KeyStoreException {
        final ArrayList<CertificateOptions> certificates = new ArrayList<>();

        certificates.add(new CertificateOptions(getPath("localhost.cer"), getPath("localhost.key")));
        certificates.add(new CertificateOptions(getPath("localhost2.cer"), getPath("localhost2.key")));
        certificates.add(new CertificateOptions(getPath("localhost3.cer"), getPath("localhost3.key")));
        certificates.add(new CertificateOptions(getPath("wildcard.cer"), getPath("wildcard.key")));

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
            .certificates(certificates)
            .password("secret")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        final KeyStoreEvent keyStoreEvent = eventRef.get();
        assertThat(keyStoreEvent).isNotNull();
        assertThat(keyStoreEvent.loaderId()).isEqualTo(cut.id());
        KeyStoreEvent.LoadEvent loadEvent = (KeyStoreEvent.LoadEvent) keyStoreEvent;
        assertThat(loadEvent.keyStore()).isNotNull();
        assertThat(loadEvent.keyStore().size()).isEqualTo(4);
        assertThat(loadEvent.password()).isEqualTo("secret");
    }

    @Test
    void should_watch_file_and_detect_changes() throws IOException, InterruptedException {
        final File tempKeyStore = File.createTempFile("gio", ".p12");
        FileCopyUtils.copy(new File(getPath("localhost.p12")), tempKeyStore);

        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(tempKeyStore.getAbsolutePath()))
            .password("secret")
            .watch(true)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        // Check the initial keystore has been properly loaded.
        assertThat(eventRef.get()).isNotNull();
        assertThat(KeyStoreUtils.getDefaultAlias(((KeyStoreEvent.LoadEvent) eventRef.get()).keyStore())).isEqualTo("localhost");

        FileCopyUtils.copy(new File(getPath("localhost2.p12")), tempKeyStore);

        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                KeyStoreEvent event = eventRef.get();
                assertThat(event).isNotNull();
                assertThat(KeyStoreUtils.getDefaultAlias(((KeyStoreEvent.LoadEvent) event).keyStore()))
                    .withFailMessage("cannot find new keystore with default alias = 'localhost2'")
                    .isEqualTo("localhost2");
            });

        cut.stop();
    }

    @Test
    void should_load_multiple_key_stores() throws KeyStoreException {
        final KeyStoreLoaderOptions options = KeyStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(getPath("localhost.p12"), getPath("localhost2.p12")))
            .password("secret")
            .defaultAlias("localhost")
            .watch(false)
            .build();

        cut = new FileKeyStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        assertThat(eventRef.get()).isNotNull();

        final KeyStoreEvent event = eventRef.get();
        assertThat(event).isNotNull();
        KeyStoreEvent.LoadEvent loadEvent = (KeyStoreEvent.LoadEvent) event;
        assertThat(loadEvent.keyStore()).isNotNull();
        assertThat(loadEvent.keyStore().size()).isEqualTo(2);
        assertThat(loadEvent.password()).isEqualTo("secret");
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/keystores/" + resource).getPath();
    }

    private List<String> getPaths(String resource) {
        return List.of(getPath(resource));
    }
}
