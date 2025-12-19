package io.gravitee.node.logging.fakes.loggers;

import io.gravitee.node.api.Node;
import io.gravitee.node.logging.LogEntry;
import io.gravitee.node.logging.LogEntryFactory;
import io.gravitee.node.logging.fakes.context.BaseExecutionContext;
import io.gravitee.node.logging.fakes.context.KafkaExecutionContext;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * A fake KafkaExecutionContextAwareLogger use to test the behavior of inheritance for Gravitee Aware Loggers.
 * It ensures all the keys for MDC enrichment are properly populated.
 */
public class KafkaExecutionContextAwareLogger extends AbstractExecutionContextAwareLogger<KafkaExecutionContext> {

    private static final Set<LogEntry<KafkaExecutionContext>> KAFKA_LOG_ENTRIES = Set.of(
        LogEntryFactory.refreshable("kafkaAttribute", KafkaExecutionContext.class, context -> context.getAttribute("kafkaAttribute"))
    );

    public KafkaExecutionContextAwareLogger(KafkaExecutionContext context, Logger logger, Node node) {
        super(context, logger, node);
    }

    @Override
    protected void registerLogEntries(Set<LogEntry<?>> entries) {
        super.registerLogEntries(entries);
        entries.addAll(KAFKA_LOG_ENTRIES);
    }

    @Override
    protected void registerLogSources(Map<Class<?>, Object> logSources) {
        super.registerLogSources(logSources);
        logSources.putIfAbsent(KafkaExecutionContext.class, context);
    }
}
