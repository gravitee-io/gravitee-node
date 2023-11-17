/*
 * *
 *  * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *         http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package io.gravitee.node.certificates.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.certificate.CertificateOptions;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FileTrustStoreLoaderFactoryTest {

    private FileTrustStoreLoaderFactory cut;

    @BeforeEach
    void before() {
        cut = new FileTrustStoreLoaderFactory();
    }

    public static Stream<Arguments> workingOptions() {
        return Stream.of(
            arguments(
                "JKS",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).paths(getPath("truststore1.jks")).build()
            ),
            arguments(
                "PKCS12",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).paths(getPath("truststore2-3.p12")).build()
            ),
            arguments(
                "PEM",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).paths(getPath("client1.crt")).build()
            )
        );
    }

    @MethodSource("workingOptions")
    @ParameterizedTest(name = "{0}")
    void should_be_able_to_handle_options(String _name, TrustStoreLoaderOptions options) {
        assertThat(cut.canHandle(options)).isTrue();
    }

    public static Stream<Arguments> nonWorkingOptions() {
        return Stream.of(
            arguments("no path PKCS12", TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).build()),
            arguments("no path JKS", TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).build()),
            arguments("no path PEM", TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).build()),
            arguments(
                "empty path PKCS12",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PKCS12).paths(List.of()).build()
            ),
            arguments(
                "empty path JKS",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_JKS).paths(List.of()).build()
            ),
            arguments(
                "empty path PEM",
                TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).paths(List.of()).build()
            )
        );
    }

    @MethodSource("nonWorkingOptions")
    @ParameterizedTest(name = "{0}")
    void should_not_be_able_to_handle_options(String _name, TrustStoreLoaderOptions options) {
        assertThat(cut.canHandle(options)).isFalse();
    }

    private static List<String> getPath(String resource) {
        return List.of(FileKeyStoreLoaderFactoryTest.class.getResource("/truststores/" + resource).getPath());
    }
}
