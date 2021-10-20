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
import io.gravitee.node.api.certificate.KeyStoreLoaderFactory;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FileKeyStoreLoaderFactory implements KeyStoreLoaderFactory {

  private static final List<String> SUPPORTED_TYPES = Arrays.asList(
    KeyStoreLoader.CERTIFICATE_FORMAT_JKS.toLowerCase(),
    KeyStoreLoader.CERTIFICATE_FORMAT_PEM.toLowerCase(),
    KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12.toLowerCase()
  );

  @Override
  public boolean canHandle(KeyStoreLoaderOptions options) {
    return (
      options.getKeyStoreType() != null &&
      SUPPORTED_TYPES.contains(options.getKeyStoreType().toLowerCase()) &&
      (
        (
          options.getKeyStorePath() != null &&
          !options.getKeyStorePath().isEmpty()
        ) ||
        (
          options.getKeyStoreCertificates() != null &&
          !options.getKeyStoreCertificates().isEmpty()
        )
      )
    );
  }

  @Override
  public KeyStoreLoader create(KeyStoreLoaderOptions options) {
    return new FileKeyStoreLoader(options);
  }
}
