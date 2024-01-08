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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreProcessingException;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FolderTrustStoreLoaderTest {

    private FolderTrustStoreLoader cut;

    @AfterEach
    public void after() {
        cut.stop();
    }

    @Test
    void should_load_pems() throws KeyStoreException {
        TrustStoreLoaderOptions options = TrustStoreLoaderOptions
            .builder()
            .paths(List.of(getPath("pems")))
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER)
            .watch(false)
            .build();

        cut = new FolderTrustStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        final KeyStoreEvent.LoadEvent keyStoreEvent = (KeyStoreEvent.LoadEvent) eventRef.get();
        assertThat(keyStoreEvent).isNotNull();
        assertThat(keyStoreEvent.loaderId()).isEqualTo(cut.id());
        assertThat(keyStoreEvent.keyStore()).isNotNull();
        assertThat(keyStoreEvent.keyStore().size()).isEqualTo(2);
        assertThat(Collections.list(keyStoreEvent.keyStore().aliases()))
            .withFailMessage("is not a truststore")
            .matches(aliases ->
                aliases
                    .stream()
                    .allMatch(alias -> {
                        try {
                            return keyStoreEvent.keyStore().isCertificateEntry(alias);
                        } catch (KeyStoreException e) {
                            throw new RuntimeException(e);
                        }
                    })
            );
    }

    @Test
    void should_watch_file_and_detect_changes() throws IOException, KeyStoreException {
        Path tempDirectory = Files.createTempDirectory("gio");
        Files
            .list(Path.of(getPath("pems")))
            .forEach(file -> {
                try {
                    Files.copy(file, tempDirectory.resolve(file.getFileName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        TrustStoreLoaderOptions options = TrustStoreLoaderOptions
            .builder()
            .paths(List.of(tempDirectory.toString()))
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER)
            .watch(true)
            .build();

        cut = new FolderTrustStoreLoader(options);

        final AtomicReference<KeyStoreEvent> eventRef = new AtomicReference<>(null);
        cut.setEventHandler(eventRef::set);

        cut.start();

        // Check the initial keystore has been properly loaded.
        assertThat(eventRef.get()).isNotNull();
        assertThat(((KeyStoreEvent.LoadEvent) eventRef.get()).keyStore()).isNotNull();
        assertThat(((KeyStoreEvent.LoadEvent) eventRef.get()).keyStore().size()).isEqualTo(2);

        Files.copy(Path.of(getPath("client3.crt")), tempDirectory.resolve("client3.crt"));

        await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                KeyStoreEvent event = eventRef.get();
                assertThat(event).isNotNull();
                assertThat(((KeyStoreEvent.LoadEvent) eventRef.get()).keyStore().size())
                    .withFailMessage("new cert should have added to the trust store to have 3 entries")
                    .isEqualTo(3);
            });

        cut.stop();
    }

    public static Stream<Arguments> badConfigs() {
        return Stream.of(
            arguments(TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER).build()),
            arguments(TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER).paths(List.of()).build()),
            arguments(
                TrustStoreLoaderOptions
                    .builder()
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER)
                    .paths(List.of(getPath("pems/client1.crt"))) // no a directory
                    .build()
            )
        );
    }

    @MethodSource("badConfigs")
    @ParameterizedTest
    void should_fail_miserably(TrustStoreLoaderOptions options) {
        cut = new FolderTrustStoreLoader(options);
        assertThatCode(() -> cut.start()).hasMessageContaining("PEM files").isInstanceOf(KeyStoreProcessingException.class);
    }

    private static String getPath(String resource) {
        return FolderTrustStoreLoaderTest.class.getResource("/truststores/" + resource).getPath();
    }
}
