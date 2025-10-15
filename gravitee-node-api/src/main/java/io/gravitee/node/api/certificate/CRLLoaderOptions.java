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

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Configuration options for CRL loaders.
 * Defines the path to CRL resources and whether to watch for changes.
 *
 * @author Guillaume SALA (guillaume.sala at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Builder
@ToString
public class CRLLoaderOptions {

    private static final boolean DEFAULT_WATCH = true;

    private final String path;

    @Builder.Default
    private final boolean watch = DEFAULT_WATCH;

    public boolean isConfigured() {
        return path != null && !path.isEmpty();
    }
}
