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
package io.gravitee.node.certificates.crl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.node.api.certificate.CRLLoaderOptions;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FolderCRLLoaderFactoryTest {

    private FolderCRLLoaderFactory cut;

    @BeforeEach
    void before() {
        cut = new FolderCRLLoaderFactory();
    }

    @Test
    void should_handle_directory_path() {
        CRLLoaderOptions options = CRLLoaderOptions.builder().path(getPath()).build();
        assertThat(cut.canHandle(options)).isTrue();
    }

    public static Stream<Arguments> invalidOptions() {
        return Stream.of(
            arguments("no path", CRLLoaderOptions.builder().build()),
            arguments("empty path", CRLLoaderOptions.builder().path("").build()),
            arguments("file path", CRLLoaderOptions.builder().path(getFilePath()).build()),
            arguments("non-existing directory", CRLLoaderOptions.builder().path("/non/existing/directory").build())
        );
    }

    @MethodSource("invalidOptions")
    @ParameterizedTest(name = "{0}")
    void should_not_handle_invalid_options(String _name, CRLLoaderOptions options) {
        assertThat(cut.canHandle(options)).isFalse();
    }

    private static String getPath() {
        return FolderCRLLoaderFactoryTest.class.getResource("/crls").getPath();
    }

    private static String getFilePath() {
        return FolderCRLLoaderFactoryTest.class.getResource("/crls/crl-empty.pem").getPath();
    }
}
