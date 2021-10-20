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

import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeyStoreLoaderOptions {

  private String keyStorePath;
  private String keyStorePassword;
  private String keyStoreType;
  private List<String> kubernetesLocations;
  private List<CertificateOptions> keyStoreCertificates;
  private boolean watch = true;
  private String defaultAlias = null;

  public String getKeyStorePath() {
    return keyStorePath;
  }

  public void setKeyStorePath(String keyStorePath) {
    this.keyStorePath = keyStorePath;
  }

  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  public void setKeyStorePassword(String keyStorePassword) {
    this.keyStorePassword = keyStorePassword;
  }

  public String getKeyStoreType() {
    return keyStoreType;
  }

  public void setKeyStoreType(String keyStoreType) {
    this.keyStoreType = keyStoreType;
  }

  public List<CertificateOptions> getKeyStoreCertificates() {
    return keyStoreCertificates;
  }

  public void setKeyStoreCertificates(
    List<CertificateOptions> keyStoreCertificates
  ) {
    this.keyStoreCertificates = keyStoreCertificates;
  }

  public List<String> getKubernetesLocations() {
    return kubernetesLocations;
  }

  public void setKubernetesLocations(List<String> kubernetesLocations) {
    this.kubernetesLocations = kubernetesLocations;
  }

  public boolean isWatch() {
    return watch;
  }

  public void setWatch(boolean watch) {
    this.watch = watch;
  }

  public String getDefaultAlias() {
    return defaultAlias;
  }

  public void setDefaultAlias(String defaultAlias) {
    this.defaultAlias = defaultAlias;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private KeyStoreLoaderOptions keyStoreLoaderOptions;

    private Builder() {
      keyStoreLoaderOptions = new KeyStoreLoaderOptions();
    }

    public Builder withKeyStorePath(String keyStorePath) {
      keyStoreLoaderOptions.setKeyStorePath(keyStorePath);
      return this;
    }

    public Builder withKeyStorePassword(String keyStorePassword) {
      keyStoreLoaderOptions.setKeyStorePassword(keyStorePassword);
      return this;
    }

    public Builder withKeyStoreType(String keyStoreType) {
      keyStoreLoaderOptions.setKeyStoreType(keyStoreType);
      return this;
    }

    public Builder withKubernetesLocations(List<String> kubernetesLocations) {
      keyStoreLoaderOptions.setKubernetesLocations(kubernetesLocations);
      return this;
    }

    public Builder withKeyStoreCertificates(
      List<CertificateOptions> keyStoreCertificates
    ) {
      keyStoreLoaderOptions.setKeyStoreCertificates(keyStoreCertificates);
      return this;
    }

    public Builder withWatch(boolean watch) {
      keyStoreLoaderOptions.setWatch(watch);
      return this;
    }

    public Builder withDefaultAlias(String defaultAlias) {
      keyStoreLoaderOptions.setDefaultAlias(defaultAlias);
      return this;
    }

    public KeyStoreLoaderOptions build() {
      return keyStoreLoaderOptions;
    }
  }
}
