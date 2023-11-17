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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreLoaderManagerTest {

    private KeyStoreLoaderManager cut;

    @Before
    public void before() {
        cut = new KeyStoreLoaderManager();
    }

    @Test
    public void shouldHaveDefaultLoaders() {
        assertEquals(2, cut.getLoaderFactories().size());
        assertTrue(
            cut
                .getLoaderFactories()
                .stream()
                .allMatch(l -> l instanceof FileKeyStoreLoaderFactory || l instanceof SelfSignedKeyStoreLoaderFactory)
        );
    }

    @Test
    public void shouldRegisterFactory() {
        final KeyStoreLoaderFactory loaderFactory = mock(KeyStoreLoaderFactory.class);
        cut.registerFactory(loaderFactory);

        assertTrue(cut.getLoaderFactories().contains(loaderFactory));
    }

    @Test
    public void shouldCreateUsingAppropriateFactory() {
        final KeyStoreLoaderFactory loaderFactory = mock(KeyStoreLoaderFactory.class);
        final KeyStoreLoader keyStoreLoader = mock(KeyStoreLoader.class);

        when(loaderFactory.canHandle(any(KeyStoreLoaderOptions.class))).thenReturn(true);
        when(loaderFactory.create(any(KeyStoreLoaderOptions.class), any())).thenReturn(keyStoreLoader);

        cut.registerFactory(loaderFactory);
        final KeyStoreLoader createdLoader = cut.create(KeyStoreLoaderOptions.builder().build(), any());

        assertEquals(keyStoreLoader, createdLoader);
    }
}
