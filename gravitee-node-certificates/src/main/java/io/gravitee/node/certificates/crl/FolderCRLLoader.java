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

import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CRL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * CRL loader that monitors a directory containing CRL files for changes. It loads all CRL files
 * from the directory and reloads them when any file in the directory changes.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class FolderCRLLoader extends AbstractFileCRLLoader {

    public FolderCRLLoader(CRLLoaderOptions options) {
        super("folder-crl", options, Paths.get(options.getPath()));
    }

    @Override
    protected boolean validatePath() {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            log.error("CRL path is not a directory: {}", path);
            notifyHandler(Collections.emptyList());
            return false;
        }
        return true;
    }

    @Override
    protected void load() {
        if (!Files.exists(path)) {
            log.warn("CRL directory does not exist: {}", path);
            notifyHandler(Collections.emptyList());
            return;
        }

        List<CRL> crls = loadFromDirectory();
        notifyHandler(crls);
    }

    @Override
    protected Path getWatchDirectory() {
        return path;
    }

    @Override
    protected boolean shouldReload(Path changedPath) {
        return true;
    }

    private List<CRL> loadFromDirectory() {
        List<CRL> crls = new ArrayList<>();

        try (Stream<Path> paths = Files.list(path)) {
            paths
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    CRL crl = loadSingleCRL(file);
                    if (crl != null) {
                        crls.add(crl);
                    }
                });
            log.info("CRL loader loaded {} CRL(s)", crls.size());
        } catch (IOException e) {
            log.error("CRL loader unable to list CRL files in directory: {}", path, e);
        }

        return crls;
    }
}
