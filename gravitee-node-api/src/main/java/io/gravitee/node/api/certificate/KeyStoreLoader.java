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
package io.gravitee.node.api.certificate;

import java.util.function.Consumer;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 *
 * Common inferface that marks objects able to read keystore or truststore and provide it to following managers. They must notifiy of their discovery and  might support internally refreshing keystore based on file/network watch.
 * They mostly are created using {@link KeyStoreLoaderFactory} espcially for platform keystores.
 *
 * To be effective a KeyStore must be resgistered to be started and stoped using a KeyStoreManager of TrustStoreManager
 */
public interface KeyStoreLoader extends IdProvider {
    String CERTIFICATE_FORMAT_JKS = "JKS";
    String CERTIFICATE_FORMAT_PEM = "PEM";
    String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";
    String CERTIFICATE_FORMAT_SELF_SIGNED = "SELF-SIGNED";
    String CERTIFICATE_FORMAT_PEM_FOLDER = "PEM-FOLDER";
    String CERTIFICATE_FORMAT_PEM_REGISTRY = "KUBERNETES-PEM-REGISTRY";

    /**
     * Must be called after creation, this method is responsible to load keystore and may start watching for updates. Eventually it will call event handler set by {@link #setEventHandler(Consumer)}
     */
    void start();

    /**
     * stop all keystore activities (watch espially and free resources if need be)
     */
    void stop();

    /**
     * KeyStoreManager or TrustStoreManager must set a handler that will be call after the keystore is loaded.
     * Most of the time first call to that handler is done during start and is synchronous, but it might later be called asynchronously.
     * @param handler {@link KeyStoreEvent} handler, must not be null
     */
    void setEventHandler(Consumer<KeyStoreEvent> handler);
}
