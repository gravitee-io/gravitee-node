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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CRL;
import java.util.Collections;
import java.util.List;
import lombok.CustomLog;

/**
 * CRL loader that monitors a single CRL file for changes. When the file is modified,
 * it reloads the CRL and notifies registered handlers.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class FileCRLLoader extends AbstractFileCRLLoader {

    public FileCRLLoader(CRLLoaderOptions options) {
        super("file-crl", options, Paths.get(options.getPath()));
    }

    @Override
    protected boolean validatePath() {
        if (Files.exists(path) && !Files.isRegularFile(path)) {
            log.error("CRL path is not a file: {}", path);
            notifyHandler(Collections.emptyList());
            return false;
        }
        return true;
    }

    @Override
    protected void load() {
        if (!Files.exists(path)) {
            log.warn("CRL file does not exist: {}", path);
            notifyHandler(Collections.emptyList());
            return;
        }

        CRL crl = loadSingleCRL(path);
        if (crl != null) {
            log.info("CRL loaded CRL from file");
            notifyHandler(List.of(crl));
        } else {
            log.error("CRL loader failed to load CRL from file: {}. The file must be a valid X.509 CRL", path);
            notifyHandler(Collections.emptyList());
        }
    }

    @Override
    protected Path getWatchDirectory() {
        Path dirToWatch = path.getParent();
        return dirToWatch != null ? dirToWatch : Paths.get(".");
    }

    @Override
    protected boolean shouldReload(Path changedPath) {
        return changedPath.equals(path.getFileName());
    }
}
