package io.gravitee.node.api.opentelemetry;

import java.util.Map;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface LoggerFactory {
    Logger createLogger(
        final String serviceInstanceId,
        final String serviceName,
        final String serviceNamespace,
        final String serviceVersion,
        final Map<String, String> additionalResourceAttributes
    );
}
