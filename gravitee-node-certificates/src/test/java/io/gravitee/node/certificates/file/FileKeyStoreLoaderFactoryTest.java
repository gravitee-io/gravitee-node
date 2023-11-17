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
package io.gravitee.node.certificates.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileKeyStoreLoaderFactoryTest {

    private FileKeyStoreLoaderFactory cut;

    @BeforeEach
    void before() {
        cut = new FileKeyStoreLoaderFactory();
    }

    public static Stream<Arguments> workingOptions() {
        return Stream.of(
            arguments(
                "JKS",
                KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).paths(getPath("localhost.jks")).build()
            ),
            arguments(
                "PKCS12",
                KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).paths(getPath("localhost.p12")).build()
            ),
            arguments(
                "PEM",
                KeyStoreLoaderOptions
                    .builder()
                    .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM)
                    .certificates(List.of(CertificateOptions.builder().certificate("ca.pem").certificate("ca.key").build()))
                    .build()
            )
        );
    }

    @MethodSource("workingOptions")
    @ParameterizedTest(name = "{0}")
    void should_be_able_to_handle_options(String _name, KeyStoreLoaderOptions options) {
        assertThat(cut.canHandle(options)).isTrue();
    }

    public static Stream<Arguments> nonWorkingOptions() {
        return Stream.of(
            arguments("no path PKCS12", KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).build()),
            arguments("no path JKS", KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).build()),
            arguments("no certs PEM", KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).build()),
            arguments(
                "empty path PKCS12",
                KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).paths(List.of()).build()
            ),
            arguments(
                "empty path JKS",
                KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).paths(List.of()).build()
            ),
            arguments(
                "empty certs PEM",
                KeyStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).certificates(List.of()).build()
            )
        );
    }

    @MethodSource("nonWorkingOptions")
    @ParameterizedTest(name = "{0}")
    void should_not_be_able_to_handle_options(String _name, KeyStoreLoaderOptions options) {
        assertThat(cut.canHandle(options)).isFalse();
    }

    private static List<String> getPath(String resource) {
        return List.of(FileKeyStoreLoaderFactoryTest.class.getResource("/keystores/" + resource).getPath());
    }
}
