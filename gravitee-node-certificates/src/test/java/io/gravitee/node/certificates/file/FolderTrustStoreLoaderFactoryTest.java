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

import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FolderTrustStoreLoaderFactoryTest {

    private FolderTrustStoreLoaderFactory cut;

    @BeforeEach
    void before() {
        this.cut = new FolderTrustStoreLoaderFactory();
    }

    @Test
    void should_be_able_to_create() {
        TrustStoreLoaderOptions options = TrustStoreLoaderOptions
            .builder()
            .type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER)
            .paths(List.of("/tmp"))
            .build();
        assertThat(cut.canHandle(options)).isTrue();
        assertThat(cut.create(options)).isNotNull();
    }

    public static Stream<Arguments> incompatibleOptions() {
        return Stream.of(
            arguments(TrustStoreLoaderOptions.builder().build()),
            arguments(TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM).build()),
            arguments(TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER).build()),
            arguments(TrustStoreLoaderOptions.builder().type(KeyStoreLoader.CERTIFICATE_FORMAT_PEM_FOLDER).paths(List.of()).build())
        );
    }

    @MethodSource("incompatibleOptions")
    @ParameterizedTest
    void should_not_be_able_to_create(TrustStoreLoaderOptions options) {
        assertThat(cut.canHandle(options)).isFalse();
    }
}
