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
