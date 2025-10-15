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
package io.gravitee.node.certificates.crl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CRL;
import java.util.ArrayList;
import java.util.List;
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
class FolderCRLLoaderTest {

    private FolderCRLLoader cut;

    @AfterEach
    public void after() {
        if (cut != null) {
            cut.stop();
        }
    }

    @Test
    void should_load_crls_from_folder() {
        CRLLoaderOptions options = CRLLoaderOptions.builder().path(getPath()).watch(false).build();

        cut = new FolderCRLLoader(options);

        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).isNotEmpty();
        assertThat(crls).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void should_watch_folder_and_detect_changes() throws IOException {
        Path tempDirectory = Files.createTempDirectory("gio");
        Files.copy(Path.of(getPath("crl-empty.pem")), tempDirectory.resolve("crl-empty.pem"));
        Files.copy(Path.of(getPath("crl-with-revocations.pem")), tempDirectory.resolve("crl-with-revocations.pem"));

        CRLLoaderOptions options = CRLLoaderOptions.builder().path(tempDirectory.toString()).watch(true).build();

        cut = new FolderCRLLoader(options);

        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).hasSizeGreaterThanOrEqualTo(2);
        int initialSize = crls.size();

        Files.delete(tempDirectory.resolve("crl-with-revocations.pem"));

        await()
            .atMost(20, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(crls.size()).withFailMessage("new CRL should have been added to the list").isGreaterThan(initialSize);
            });

        cut.stop();
    }

    @Test
    void should_handle_missing_folder() {
        CRLLoaderOptions options = CRLLoaderOptions.builder().path("/path-to-unknown").watch(false).build();

        cut = new FolderCRLLoader(options);

        List<CRL> crls = new ArrayList<>();
        cut.setEventHandler(crls::addAll);
        cut.start();

        assertThat(crls).isEmpty();
    }

    private static String getPath() {
        return FolderCRLLoaderTest.class.getResource("/crls").getPath();
    }

    private static String getPath(String resource) {
        return FolderCRLLoaderTest.class.getResource("/crls/" + resource).getPath();
    }
}
