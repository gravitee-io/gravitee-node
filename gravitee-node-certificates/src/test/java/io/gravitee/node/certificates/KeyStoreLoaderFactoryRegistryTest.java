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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.certificates.file.FileKeyStoreLoaderFactory;
import io.gravitee.node.certificates.file.FileTrustStoreLoaderFactory;
import io.gravitee.node.certificates.selfsigned.SelfSignedKeyStoreLoaderFactory;
import io.gravitee.node.certificates.spring.NodeCertificatesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class KeyStoreLoaderFactoryRegistryTest {

    private DefaultKeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> cutKS;
    private DefaultKeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> cutTS;

    @Before
    public void before() {
        cutKS = new NodeCertificatesConfiguration().keyStoreLoaderFactoryRegistry();
        cutTS = new NodeCertificatesConfiguration().trustStoreLoaderFactoryRegistry();
    }

    @Test
    public void should_have_default_loaders() {
        assertThat(cutKS.getLoaderFactories())
            .hasSize(2)
            .hasOnlyElementsOfTypes(FileKeyStoreLoaderFactory.class, SelfSignedKeyStoreLoaderFactory.class);
        assertThat(cutTS.getLoaderFactories()).hasSize(1).hasOnlyElementsOfTypes(FileTrustStoreLoaderFactory.class);
    }

    @Test
    public void should_register_factory() {
        final KeyStoreLoaderFactory<KeyStoreLoaderOptions> loaderFactory = mock(KeyStoreLoaderFactory.class);
        cutKS.registerFactory(loaderFactory);

        assertTrue(cutKS.getLoaderFactories().contains(loaderFactory));
    }

    @Test
    public void should_create_using_appropriate_factory() {
        final KeyStoreLoaderFactory<KeyStoreLoaderOptions> loaderFactory = mock(KeyStoreLoaderFactory.class);
        final KeyStoreLoader keyStoreLoader = mock(KeyStoreLoader.class);

        when(loaderFactory.canHandle(any(KeyStoreLoaderOptions.class))).thenReturn(true);
        when(loaderFactory.create(any(KeyStoreLoaderOptions.class))).thenReturn(keyStoreLoader);

        cutKS.registerFactory(loaderFactory);
        final KeyStoreLoader createdLoader = cutKS.createLoader(KeyStoreLoaderOptions.builder().build(), any());

        assertEquals(keyStoreLoader, createdLoader);
    }
}
