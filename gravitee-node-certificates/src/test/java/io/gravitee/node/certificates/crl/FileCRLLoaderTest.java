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
package io.gravitee.node.certificates.crl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.CRL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileCRLLoaderTest {

    FileCRLLoader cut;

    @AfterEach
    void after() {
        if (cut != null) {
            cut.stop();
        }
    }

    @Test
    void should_load_crl_file() {
        cut = new FileCRLLoader(CRLLoaderOptions.builder().path(getPath("crl-empty.pem")).watch(false).build());
        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).hasSize(1);
        assertThat(crls.get(0)).isNotNull();
    }

    @Test
    void should_load_crl_file_in_der_format() {
        cut = new FileCRLLoader(CRLLoaderOptions.builder().path(getPath("crl-with-revocations.der")).watch(false).build());
        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).hasSize(1);
        assertThat(crls.get(0)).isNotNull();
    }

    @Test
    void should_watch_and_trigger_reload_when_file_changes() throws Exception {
        Path tempDirectory = Files.createTempDirectory("gio");
        Path target = tempDirectory.resolve("crl-empty.pem");
        Files.copy(Path.of(getPath("crl-empty.pem")), target);

        cut = new FileCRLLoader(CRLLoaderOptions.builder().path(target.toString()).watch(true).build());
        List<CRL> crls = new CopyOnWriteArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).isNotEmpty();

        int sizeBeforeOverride = crls.size();

        Files.copy(Path.of(getPath("crl-empty.pem")), target, StandardCopyOption.REPLACE_EXISTING);

        await()
            .atMost(20, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(crls).withFailMessage("no events triggered following CRL override").hasSizeGreaterThan(sizeBeforeOverride);
            });

        cut.stop();
    }

    @Test
    void should_handle_missing_file() {
        cut = new FileCRLLoader(CRLLoaderOptions.builder().path("/path-to-unknown.crl").watch(false).build());
        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).isEmpty();
    }

    @Test
    void should_handle_invalid_crl_file() throws IOException {
        Path tempFile = Files.createTempFile("gio", ".crl");
        Files.write(tempFile, new byte[0]);

        cut = new FileCRLLoader(CRLLoaderOptions.builder().path(tempFile.toString()).watch(false).build());
        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).isEmpty();
    }

    private String getPath(String resource) {
        return this.getClass().getResource("/crls/" + resource).getPath();
    }
}
