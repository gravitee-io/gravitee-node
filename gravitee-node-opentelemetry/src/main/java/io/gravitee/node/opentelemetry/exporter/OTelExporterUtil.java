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
package io.gravitee.node.opentelemetry.exporter;

import java.net.URI;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/exporter/otlp/OTelExporterUtil.java">OTelExporterUtil.java</a>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OTelExporterUtil {

    public static int getPort(URI uri) {
        int originalPort = uri.getPort();
        if (originalPort > -1) {
            return originalPort;
        }

        if (isHttps(uri)) {
            return 443;
        }
        return 80;
    }

    public static boolean isHttps(URI uri) {
        return "https".equals(uri.getScheme().toLowerCase(Locale.ROOT)) || "grpcs".equals(uri.getScheme().toLowerCase(Locale.ROOT));
    }
}
