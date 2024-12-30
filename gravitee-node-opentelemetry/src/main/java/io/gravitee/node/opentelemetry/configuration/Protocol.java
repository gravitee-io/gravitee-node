/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.opentelemetry.configuration;

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public enum Protocol {
    UNKNOWN("unknown"),
    GRPC("grpc"),
    HTTP_PROTOBUF("http/protobuf"),
    HTTP("http");

    private final String value;

    private static final Map<String, Protocol> MAP = Map.of(
        UNKNOWN.value,
        UNKNOWN,
        GRPC.value,
        GRPC,
        HTTP_PROTOBUF.value,
        HTTP_PROTOBUF,
        HTTP.value,
        HTTP
    );

    public static Protocol fromValue(final String value) {
        if (value != null && MAP.containsKey(value)) {
            return MAP.get(value);
        }
        return UNKNOWN;
    }
}
