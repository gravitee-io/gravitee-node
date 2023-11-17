/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.node.certificates.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileTrustStoreLoaderTest {

    FileTrustStoreLoader cut;

    @Test
    void should_load_jks_truststore() throws KeyStoreException {
        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions
                    .builder()
                    .type("JKS")
                    .password("secret")
                    .watch(false)
                    .paths(List.of(getPath("truststore1.jks")))
                    .build()
            );
        List<KeyStoreEvent> events = new ArrayList<>();
        cut.setEventHandler(events::add);
        cut.start();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
        assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(0)).keyStore().aliases())).hasSize(1);
    }

    @Test
    void should_load_p12_truststore() throws KeyStoreException {
        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions
                    .builder()
                    .type("PKCS12")
                    .password("secret")
                    .watch(false)
                    .paths(List.of(getPath("truststore2-3.p12")))
                    .build()
            );
        List<KeyStoreEvent> events = new ArrayList<>();
        cut.setEventHandler(events::add);
        cut.start();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
        assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(0)).keyStore().aliases())).hasSize(2);
    }

    @Test
    void should_load_pem_truststore() throws KeyStoreException {
        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions
                    .builder()
                    .type("PEM")
                    .password("secret")
                    .watch(false)
                    .paths(List.of(getPath("client1.crt"), getPath("client2.crt")))
                    .build()
            );
        List<KeyStoreEvent> events = new ArrayList<>();
        cut.setEventHandler(events::add);
        cut.start();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
        assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(0)).keyStore().aliases())).hasSize(2);
    }

    @Test
    void should_load_several_truststores() throws KeyStoreException {
        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions
                    .builder()
                    .type("JKS")
                    .password("secret")
                    .watch(false)
                    .paths(List.of(getPath("truststore1.jks"), getPath("truststore2-3.jks")))
                    .build()
            );
        List<KeyStoreEvent> events = new ArrayList<>();
        cut.setEventHandler(events::add);
        cut.start();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
        assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(0)).keyStore().aliases())).hasSize(3);
    }

    @Test
    void should_watch_and_trigger_reload_when_file_changes() throws IOException, KeyStoreException {
        Path tempDirectory = Files.createTempDirectory("gio");
        Path target = tempDirectory.resolve("truststore1.jks");
        Files.copy(Path.of(getPath("truststore1.jks")), target);

        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions.builder().type("JKS").password("secret").watch(true).paths(List.of(target.toString())).build()
            );
        List<KeyStoreEvent> events = new ArrayList<>();
        cut.setEventHandler(events::add);
        cut.start();

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
        assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(0)).keyStore().aliases())).hasSize(1);

        // change target file
        Files.copy(Path.of(getPath("truststore2-3.jks")), target, StandardCopyOption.REPLACE_EXISTING);

        await()
            .atMost(2, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(events).withFailMessage("no events triggered following keystore override").hasSize(2);
                assertThat(events.get(1)).isInstanceOf(KeyStoreEvent.LoadEvent.class);
                assertThat(Collections.list(((KeyStoreEvent.LoadEvent) events.get(1)).keyStore().aliases())).hasSize(2); // new keystore has 2 aliases
            });

        cut.stop();
    }

    @Test
    void should_fail_load_truststore_with_wrong_password() {
        cut =
            new FileTrustStoreLoader(
                TrustStoreLoaderOptions
                    .builder()
                    .type("JKS")
                    .password("this ain't the right password")
                    .watch(false)
                    .paths(List.of(getPath("truststore1.jks")))
                    .build()
            );
        cut.setEventHandler(ignore -> {});
        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_start_on_missing_file() {
        final TrustStoreLoaderOptions options = TrustStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of("/path-to-unknown.p12"))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileTrustStoreLoader(options);
        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_not_start_on_invalid_key_store() throws IOException {
        final File tempKeyStore = File.createTempFile("gio", ".p12");
        FileCopyUtils.copy(new byte[0], tempKeyStore);

        final TrustStoreLoaderOptions options = TrustStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12)
            .paths(List.of(tempKeyStore.getAbsolutePath()))
            .password("secret")
            .watch(false)
            .build();

        cut = new FileTrustStoreLoader(options);
        assertThatCode(() -> cut.start()).isInstanceOf(IllegalArgumentException.class);
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/truststores/" + resource).getPath();
    }
}
