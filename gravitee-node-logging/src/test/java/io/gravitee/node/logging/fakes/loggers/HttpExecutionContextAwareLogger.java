package io.gravitee.node.logging.fakes.loggers;

import io.gravitee.node.api.Node;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.node.logging.LogEntryFactory;
import io.gravitee.node.logging.fakes.context.HttpExecutionContext;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * A fake HttpExecutionContextAwareLogger use to test the behavior of inheritance for Gravitee Aware Loggers.
 * It ensures all the keys for MDC enrichment are properly populated.
 */
public class HttpExecutionContextAwareLogger extends AbstractExecutionContextAwareLogger<HttpExecutionContext> {

    private static final Set<LogEntry<HttpExecutionContext>> HTTP_LOG_ENTRIES = Set.of(
        LogEntryFactory.refreshable("httpAttribute", HttpExecutionContext.class, context -> context.getAttribute("httpAttribute"))
    );

    public HttpExecutionContextAwareLogger(HttpExecutionContext context, Logger logger, Node node) {
        super(context, logger, node);
    }

    @Override
    protected void registerLogEntries(Set<LogEntry<?>> entries) {
        super.registerLogEntries(entries);
        entries.addAll(HTTP_LOG_ENTRIES);
    }

    @Override
    protected void registerLogSources(Map<Class<?>, Object> logSources) {
        super.registerLogSources(logSources);
        logSources.putIfAbsent(HttpExecutionContext.class, context);
    }
}
