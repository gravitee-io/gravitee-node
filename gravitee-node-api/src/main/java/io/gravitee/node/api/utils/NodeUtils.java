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
package io.gravitee.node.api.utils;

import io.gravitee.common.util.Version;
import io.gravitee.node.api.Node;

public class NodeUtils {

    public static String userAgent(Node node) {
        return node == null
            ? null
            : "Gravitee.io/" +
            Version.RUNTIME_VERSION.MAJOR_VERSION +
            " (" +
            node.application() +
            "; " +
            node.name() +
            "; " +
            node.id() +
            ")";
    }
}
