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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.certificate.*;
import io.gravitee.node.certificates.file.FileKeyStoreLoaderFactory;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import io.gravitee.node.certificates.file.FolderTrustStoreLoaderFactory;
import io.gravitee.node.certificates.selfsigned.SelfSignedKeyStoreLoaderFactory;
import io.gravitee.node.certificates.spring.NodeCertificatesConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class KeyStoreLoaderFactoryRegistryTest {

    private DefaultKeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> cutKS;
    private DefaultKeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> cutTS;

    @BeforeEach
    void before() {
        cutKS = new NodeCertificatesConfiguration().keyStoreLoaderFactoryRegistry();
        cutTS = new NodeCertificatesConfiguration().trustStoreLoaderFactoryRegistry();
    }

    @Test
    void should_have_default_loaders() {
        assertThat(cutKS.getLoaderFactories())
            .hasSize(2)
            .hasOnlyElementsOfTypes(FileKeyStoreLoaderFactory.class, SelfSignedKeyStoreLoaderFactory.class);
        assertThat(cutTS.getLoaderFactories())
            .hasSize(2)
            .hasOnlyElementsOfTypes(FileTrustStoreLoaderFactory.class, FolderTrustStoreLoaderFactory.class);
    }

    /**
     * createLoader is called for every server whether or not it is secured, and the options are always built
     * with a default type and nothing to load from. That has to stay quiet: warning on it would put two lines
     * per plain listener in every boot log, which is how operators learn to ignore warnings.
     */
    @Test
    void should_return_no_op_loader_when_no_store_is_configured() {
        final KeyStoreLoaderOptions unconfigured = KeyStoreLoaderOptions.builder().type("JKS").build();

        assertThat(cutKS.createLoader(unconfigured)).isInstanceOf(DefaultKeyStoreLoaderFactoryRegistry.NoOpKeyStoreLoader.class);
        assertThat(cutTS.createLoader(TrustStoreLoaderOptions.builder().type("JKS").build()))
            .isInstanceOf(DefaultKeyStoreLoaderFactoryRegistry.NoOpKeyStoreLoader.class);
    }

    @Test
    void should_return_no_op_loader_when_a_configured_store_matches_no_factory() {
        // A source is named, so something was meant to load: the registry has nothing for this pair and says so.
        final KeyStoreLoaderOptions unmatched = KeyStoreLoaderOptions
            .builder()
            .type("BCFKS")
            .secretLocation("secret://kubernetes/my-tls-secret")
            .build();

        assertThat(cutKS.createLoader(unmatched)).isInstanceOf(DefaultKeyStoreLoaderFactoryRegistry.NoOpKeyStoreLoader.class);
    }

    @Test
    void should_register_factory() {
        final KeyStoreLoaderFactory<KeyStoreLoaderOptions> loaderFactory = mock(KeyStoreLoaderFactory.class);
        cutKS.registerFactory(loaderFactory);

        assertThat(cutKS.getLoaderFactories()).contains(loaderFactory);
    }

    @Test
    void should_create_using_appropriate_factory() {
        final KeyStoreLoaderFactory<KeyStoreLoaderOptions> loaderFactory = mock(KeyStoreLoaderFactory.class);
        final KeyStoreLoader keyStoreLoader = mock(KeyStoreLoader.class);

        when(loaderFactory.canHandle(any(KeyStoreLoaderOptions.class))).thenReturn(true);
        when(loaderFactory.create(any(KeyStoreLoaderOptions.class))).thenReturn(keyStoreLoader);

        cutKS.registerFactory(loaderFactory);
        final KeyStoreLoader createdLoader = cutKS.createLoader(KeyStoreLoaderOptions.builder().build());

        assertThat(keyStoreLoader).isEqualTo(createdLoader);
    }

    @Test
    void should_fallback_to_noop() {
        final KeyStoreLoader createdKSLoader = cutKS.createLoader(KeyStoreLoaderOptions.builder().build());
        final KeyStoreLoader createdTSLoader = cutTS.createLoader(TrustStoreLoaderOptions.builder().build());
        assertThat(createdKSLoader.id()).isEqualTo("no-op");
        assertThat(createdTSLoader.id()).isEqualTo("no-op");
    }

    @Test
    void should_ensure_several_matching_factories_generates_an_error() {
        // add and always matching factory
        cutKS.registerFactory(
            new KeyStoreLoaderFactory<>() {
                @Override
                public boolean canHandle(KeyStoreLoaderOptions options) {
                    return true;
                }

                @Override
                public KeyStoreLoader create(KeyStoreLoaderOptions options) {
                    return null;
                }
            }
        );
        KeyStoreLoaderOptions selfSigned = KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED).build();

        assertThatCode(() -> cutKS.createLoader(selfSigned)).isInstanceOf(IllegalArgumentException.class);
    }
}
