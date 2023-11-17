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

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreManager;
import io.gravitee.node.api.certificate.TrustStoreLoader;
import java.util.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BaseKeyStoreManager implements KeyStoreManager {

    protected boolean enableSni;
    protected final ReloadableKeyManager keyManager;
    protected final ReloadableCertificateManager certManager;
    protected final Set<String> keyStoreServerIds;
    protected final Set<String> trustStoreServerIds;

    public BaseKeyStoreManager(String serverId, boolean enableSni) {
        this.enableSni = enableSni;
        this.keyManager = new ReloadableKeyManager(serverId);
        this.certManager = new ReloadableCertificateManager(serverId);
        keyStoreServerIds = Collections.synchronizedSet(new HashSet<>());
        trustStoreServerIds = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void registerLoader(KeyStoreLoader keyStoreLoader, String serverId) {
        Objects.requireNonNull(keyStoreLoader, "KeyStoreLoader cannot be null");
        if (this.keyStoreServerIds.add(serverId)) {
            keyStoreLoader.addListener(keyStoreBundle -> {
                try {
                    keyManager.load(
                        keyStoreBundle.getDefaultAlias(),
                        keyStoreBundle.getKeyStore(),
                        keyStoreBundle.getPassword(),
                        enableSni
                    );
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to load the keystore", e);
                }
            });
            keyStoreLoader.start();
        }
    }

    @Override
    public void registerLoader(TrustStoreLoader trustStoreLoader, String serverId) {
        Objects.requireNonNull(trustStoreLoader, "TrustStoreLoader cannot be null");
        if (this.trustStoreServerIds.add(serverId)) {
            trustStoreLoader.addListener(trustStore -> {
                try {
                    certManager.load(trustStore);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Unable to load the keystore", e);
                }
            });

            trustStoreLoader.start();
        }
    }
}
