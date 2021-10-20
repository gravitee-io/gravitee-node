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

import static io.gravitee.common.util.KeyStoreUtils.DEFAULT_ALIAS;
import static org.junit.Assert.*;

import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.security.KeyStoreException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
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
    cut =
      new SelfSignedKeyStoreLoader(
        KeyStoreLoaderOptions
          .builder()
          .withKeyStoreType(KeyStoreLoader.CERTIFICATE_FORMAT_SELF_SIGNED)
          .build()
      );
  }

  @Test
  public void shouldGenerateSelfSignedCertificate() throws KeyStoreException {
    final AtomicReference<KeyStoreBundle> bundleRef = new AtomicReference<>(
      null
    );

    cut.addListener(bundleRef::set);

    cut.start();

    final KeyStoreBundle bundle = bundleRef.get();

    assertNotNull(bundle);
    assertNotNull(bundle.getKeyStore());
    assertEquals(1, bundle.getKeyStore().size());
    assertNotNull(bundle.getKeyStore().getCertificate(DEFAULT_ALIAS));
  }
}
