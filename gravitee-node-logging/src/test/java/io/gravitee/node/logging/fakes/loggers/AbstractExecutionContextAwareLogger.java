package io.gravitee.node.logging.fakes.loggers;

import io.gravitee.node.api.Node;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.node.logging.LogEntryFactory;
import io.gravitee.node.logging.NodeAwareLogger;
import io.gravitee.node.logging.fakes.context.BaseExecutionContext;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * A fake AbstractExecutionContextAwareLogger use to test the behavior of inheritance for Gravitee Aware Loggers.
 * It ensures all the keys for MDC enrichment are properly populated.
 */
public abstract class AbstractExecutionContextAwareLogger<C extends BaseExecutionContext> extends NodeAwareLogger {

    private static final Set<LogEntry<BaseExecutionContext>> BASE_LOG_ENTRIES = Set.of(
        LogEntryFactory.cached("api", BaseExecutionContext.class, context -> context.getAttribute("api")),
        LogEntryFactory.refreshable("application", BaseExecutionContext.class, context -> context.getAttribute("application"))
    );

    protected final C context;
    private Set<LogEntry<BaseExecutionContext>> mergedEntries;

    public AbstractExecutionContextAwareLogger(C context, Logger logger, Node node) {
        super(node, logger);
        this.context = context;
    }

    @Override
    protected void registerLogEntries(Set<LogEntry<?>> entries) {
        super.registerLogEntries(entries);
        entries.addAll(BASE_LOG_ENTRIES);
    }

    @Override
    protected void registerLogSources(Map<Class<?>, Object> logSources) {
        super.registerLogSources(logSources);
        logSources.putIfAbsent(BaseExecutionContext.class, context);
    }
}
