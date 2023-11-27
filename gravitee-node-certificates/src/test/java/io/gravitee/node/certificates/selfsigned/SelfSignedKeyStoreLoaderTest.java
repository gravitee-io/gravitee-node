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
package io.gravitee.node.certificates.selfsigned;

import static io.gravitee.common.util.KeyStoreUtils.DEFAULT_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import io.gravitee.node.api.certificate.KeyStoreEvent;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SelfSignedKeyStoreLoaderTest {

    private SelfSignedKeyStoreLoader cut;

    @Before
    public void before() {
        cut = new SelfSignedKeyStoreLoader();
    }

    @Test
    public void shouldGenerateSelfSignedCertificate() throws KeyStoreException {
        final AtomicReference<KeyStoreEvent> bundleRef = new AtomicReference<>(null);

        cut.setEventHandler(bundleRef::set);
        cut.start();

        final KeyStoreEvent event = bundleRef.get();

        assertNotNull(event);
        assertThat(event.loaderId()).isNotBlank();
        assertNotNull(event.keyStore());
        assertThat(event.keyStore().size()).isEqualTo(1);
        assertThat(event.keyStore().getCertificate(DEFAULT_ALIAS)).isNotNull();
    }
}
