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
package io.gravitee.node.logging;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.node.api.Node;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;

/**
 * The NodeAwareLogger class is a custom implementation of the SLF4J Logger interface
 * that integrates node-specific context into logging operations. This allows the logger
 * to enrich log entries with metadata related to a specific {@link Node}, enhancing
 * observability and debugging capabilities in distributed or multi-node environments.
 *
 * Fields:
 * - NODE_LOG_ENTRIES: A collection of log entries that capture node-specific metadata.
 * - node: The node instance associated with this logger, used for providing context.
 * - delegateLogger: The underlying SLF4J Logger to which log messages are delegated.
 * - logEntries: A set of log entry mappings for dynamic MDC (Mapped Diagnostic Context) enrichment.
 *
 * Constructor:
 * - NodeAwareLogger(Node node, Logger logger):
 *   Constructs a NodeAwareLogger instance with the provided {@link Node} and SLF4J {@link Logger}.
 *   The node provides context-specific metadata, and the logger serves as the delegate for log operations.
 *
 *   Each log operation dynamically incorporates the enriched context from the node or other sources
 *   into the MDC before delegating the message to the underlying logger.
 */
public class NodeAwareLogger implements Logger {

    private static final Set<LogEntry<Node>> NODE_LOG_ENTRIES = Set.of(
        LogEntryFactory.cached("nodeId", Node.class, Node::id),
        LogEntryFactory.cached("nodeHostname", Node.class, Node::hostname),
        LogEntryFactory.cached("nodeApplication", Node.class, Node::application)
    );
    public static final String UNKNOWN = "unknown";

    private Node node;
    protected final Logger delegateLogger;
    private final Set<LogEntry<?>> logEntries;
    /**
     * A shared reference to a {@link Node} supplier, used for lazy context initialization.
     * <p>
     * This reference is essential because loggers are often instantiated statically (e.g., via Lombok's {@code @CustomLog})
     * at class-loading time, which occurs before the {@link Node} is actually created or injected.
     * </p>
     * <p>
     * The logger follows a lazy resolution strategy:
     * <ul>
     *     <li><b>Before initialization:</b> It holds a reference to this atomic container. Log calls proceed without enrichment.</li>
     *     <li><b>Upon initialization:</b> As soon as a supplier is provided to the factory, the logger resolves the node
     *     on the next log call and caches it locally for future use, ensuring zero overhead once the context is ready.</li>
     * </ul>
     */
    private final AtomicReference<Supplier<Node>> nodeSupplierRef;

    /**
     * Extracts the delegate logger from the given logger if it is an instance of {@code NodeAwareLogger}.
     * If the provided logger is not a {@code NodeAwareLogger}, the method returns the original logger.
     * It avoids enriching MDC twice when calling {@code withLoggingContext(Runnable logAction)}
     *
     * @param logger the logger instance from which to extract the delegate logger
     * @return the delegate logger if the input logger is an instance of {@code NodeAwareLogger};
     *         otherwise, returns the original logger
     */
    private static Logger extractDelegateLogger(Logger logger) {
        return logger instanceof NodeAwareLogger nodeAwareLogger ? nodeAwareLogger.delegateLogger : logger;
    }

    /**
     * Constructs a NodeAwareLogger instance that associates a specific {@link Node}
     * with the provided {@link Logger}. This constructor ensures that the node and
     * logger instances are not null before proceeding. The logger serves as the
     * primary logging mechanism, while the node provides contextual information to
     * enhance log output.
     *
     * @param node the {@link Node} instance to be associated with this logger.
     *             It serves as a contextual reference for logging operations.
     *             Must not be null.
     * @param logger the {@link Logger} instance to which logging operations
     *               are delegated. This logger executes the actual logging logic.
     *               Must not be null.
     */
    public NodeAwareLogger(Node node, Logger logger) {
        Objects.requireNonNull(node, "Node must not be null");
        Objects.requireNonNull(logger, "Delegate logger must not be null");
        this.node = node;
        this.delegateLogger = extractDelegateLogger(logger);
        this.nodeSupplierRef = new AtomicReference<>(() -> node);
        Set<LogEntry<?>> logEntries = new HashSet<>();
        registerLogEntries(logEntries);
        this.logEntries = Set.copyOf(logEntries);
    }

    /**
     * Constructs a NodeAwareLogger instance that associates a specific {@link Node} with the provided logger.
     * This constructor ensures both the {@code nodeSupplierRef} and {@code logger} are not null before proceeding.
     * The logger acts as the foundational logging mechanism, while {@code nodeSupplierRef} supplies or retrieves
     * the associated {@code Node}.
     * Using an atomic reference allows for lazy initialization of the node instance, which is useful since NodeAwareLogger is instantiated statically, and thus, before {@code Node} initialization.
     *
     * @param nodeSupplierRef an {@link AtomicReference} containing a {@link Supplier} of {@link Node}
     *                        instances. This reference is used to provide the {@link Node} associated with logging.
     * @param logger the {@link Logger} instance to which logging operations are delegated. It serves as the main
     *               logging interface for this class.
     */
    public NodeAwareLogger(AtomicReference<Supplier<Node>> nodeSupplierRef, Logger logger) {
        Objects.requireNonNull(nodeSupplierRef, "Factory supplier provider must not be null");
        Objects.requireNonNull(logger, "Delegate logger must not be null");
        this.nodeSupplierRef = nodeSupplierRef;
        this.delegateLogger = extractDelegateLogger(logger);
        Set<LogEntry<?>> logEntries = new HashSet<>();
        registerLogEntries(logEntries);
        this.logEntries = Set.copyOf(logEntries);
    }

    /**
     * Registers log sources into the provided map. This method attempts to ensure that a log source
     * representing the {@link Node} class is added to the map. If the node instance is not available,
     * it will attempt to create one using the supplier stored in {@code nodeSupplierRef}.
     * Attempts to initialize the node using the current supplier if it is null.
     *
     * @param logSources a map where keys represent the class types of log sources and values represent
     *                   their corresponding instances. This is used to register log source instances
     *                   associated with specific classes.
     */
    protected void registerLogSources(Map<Class<?>, Object> logSources) {
        if (node == null) {
            Supplier<Node> currentSupplier = nodeSupplierRef.get();
            if (currentSupplier != null) {
                try {
                    this.node = currentSupplier.get();
                } catch (Exception e) {
                    // Do not log anything here, would generate noise
                }
            }
        }

        if (node != null) {
            logSources.putIfAbsent(Node.class, node);
        }
    }

    /**
     * Registers a set of log entries into the provided set of registered entries.
     * This method ensures all log entries defined in {@code NODE_LOG_ENTRIES} are added
     * to the provided set.
     *
     * @param registeredEntries a set of {@link LogEntry} instances where the log entries
     *                          defined in {@code NODE_LOG_ENTRIES} will be registered.
     */
    protected void registerLogEntries(Set<LogEntry<?>> registeredEntries) {
        registeredEntries.addAll(NODE_LOG_ENTRIES);
    }

    /**
     * Clears all entries from the NODE_LOG_ENTRIES collection by invoking the reset
     * method on each LogEntry instance contained within the collection. This method
     * is intended for internal use within test environments to ensure a clean state.
     */
    @VisibleForTesting
    protected static void flushLogEntry() {
        NODE_LOG_ENTRIES.forEach(LogEntry::reset);
    }

    /**
     * Attempts to provide a source object for the given log entry by checking registered log sources.
     * If no matching log source is found, a warning is logged, and an empty {@code Optional} is returned.
     *
     * @param logEntry the log entry for which the source object is required. The source type
     *                 is determined by the {@code sourceType()} method of the provided log entry.
     * @param logSources a map where keys represent the classes of log sources
     *                   and values represent their respective instances.
     * @return an {@code Optional} containing the corresponding log source object if found,
     * or an empty {@code Optional} if no matching log source is available.
     */
    private Optional<Object> provideLogSource(LogEntry<?> logEntry, Map<Class<?>, Object> logSources) {
        return Optional.ofNullable(logSources.get(logEntry.sourceType()));
    }

    private void withLoggingContext(Runnable logAction) {
        // Gather log sources from child implementations to provide LogEntry a way to be resolved
        final Map<Class<?>, Object> logSources = new HashMap<>();
        registerLogSources(logSources);

        logEntries.forEach(logEntry ->
            provideLogSource(logEntry, logSources)
                .ifPresentOrElse(
                    logSource -> {
                        String logValue = logEntry.resolve(logSource);
                        if (logValue != null) {
                            MDC.put(logEntry.getKey(), logValue);
                        }
                    },
                    () -> MDC.put(logEntry.getKey(), UNKNOWN)
                )
        );
        try {
            logAction.run();
        } finally {
            logEntries.forEach(entry -> MDC.remove(entry.getKey()));
        }
    }

    @Override
    public String getName() {
        return delegateLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegateLogger.isTraceEnabled();
    }

    @Override
    public void trace(String message) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(message));
        }
    }

    @Override
    public void trace(String message, Object arg) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(message, arg));
        }
    }

    @Override
    public void trace(String message, Object arg1, Object arg2) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(message, arg1, arg2));
        }
    }

    @Override
    public void trace(String message, Object... args) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(message, args));
        }
    }

    @Override
    public void trace(String message, Throwable throwable) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(message, throwable));
        }
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegateLogger.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String message) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(marker, message));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object arg) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(marker, message, arg));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object arg1, Object arg2) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(marker, message, arg1, arg2));
        }
    }

    @Override
    public void trace(Marker marker, String message, Object... args) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(marker, message, args));
        }
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        if (delegateLogger.isTraceEnabled()) {
            withLoggingContext(() -> delegateLogger.trace(marker, message, throwable));
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return delegateLogger.isDebugEnabled();
    }

    @Override
    public void debug(String message) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(message));
        }
    }

    @Override
    public void debug(String message, Object arg) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(message, arg));
        }
    }

    @Override
    public void debug(String message, Object arg1, Object arg2) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(message, arg1, arg2));
        }
    }

    @Override
    public void debug(String message, Object... args) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(message, args));
        }
    }

    @Override
    public void debug(String message, Throwable throwable) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(message, throwable));
        }
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegateLogger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String message) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(marker, message));
        }
    }

    @Override
    public void debug(Marker marker, String message, Object arg) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(marker, message, arg));
        }
    }

    @Override
    public void debug(Marker marker, String message, Object arg1, Object arg2) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(marker, message, arg1, arg2));
        }
    }

    @Override
    public void debug(Marker marker, String message, Object... args) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(marker, message, args));
        }
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        if (delegateLogger.isDebugEnabled()) {
            withLoggingContext(() -> delegateLogger.debug(marker, message, throwable));
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return delegateLogger.isInfoEnabled();
    }

    @Override
    public void info(String message) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(message));
        }
    }

    @Override
    public void info(String message, Object arg) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(message, arg));
        }
    }

    @Override
    public void info(String message, Object arg1, Object arg2) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(message, arg1, arg2));
        }
    }

    @Override
    public void info(String message, Object... args) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(message, args));
        }
    }

    @Override
    public void info(String message, Throwable throwable) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(message, throwable));
        }
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegateLogger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String message) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(marker, message));
        }
    }

    @Override
    public void info(Marker marker, String message, Object arg) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(marker, message, arg));
        }
    }

    @Override
    public void info(Marker marker, String message, Object arg1, Object arg2) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(marker, message, arg1, arg2));
        }
    }

    @Override
    public void info(Marker marker, String message, Object... args) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(marker, message, args));
        }
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        if (delegateLogger.isInfoEnabled()) {
            withLoggingContext(() -> delegateLogger.info(marker, message, throwable));
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return delegateLogger.isWarnEnabled();
    }

    @Override
    public void warn(String message) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(message));
        }
    }

    @Override
    public void warn(String message, Object arg) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(message, arg));
        }
    }

    @Override
    public void warn(String message, Object... args) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(message, args));
        }
    }

    @Override
    public void warn(String message, Object arg1, Object arg2) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(message, arg1, arg2));
        }
    }

    @Override
    public void warn(String message, Throwable throwable) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(message, throwable));
        }
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegateLogger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String message) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(marker, message));
        }
    }

    @Override
    public void warn(Marker marker, String message, Object arg) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(marker, message, arg));
        }
    }

    @Override
    public void warn(Marker marker, String message, Object arg1, Object arg2) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(marker, message, arg1, arg2));
        }
    }

    @Override
    public void warn(Marker marker, String message, Object... args) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(marker, message, args));
        }
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        if (delegateLogger.isWarnEnabled()) {
            withLoggingContext(() -> delegateLogger.warn(marker, message, throwable));
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return delegateLogger.isErrorEnabled();
    }

    @Override
    public void error(String message) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(message));
        }
    }

    @Override
    public void error(String message, Object arg) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(message, arg));
        }
    }

    @Override
    public void error(String message, Object arg1, Object arg2) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(message, arg1, arg2));
        }
    }

    @Override
    public void error(String message, Object... args) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(message, args));
        }
    }

    @Override
    public void error(String message, Throwable throwable) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(message, throwable));
        }
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegateLogger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String message) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(marker, message));
        }
    }

    @Override
    public void error(Marker marker, String message, Object arg) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(marker, message, arg));
        }
    }

    @Override
    public void error(Marker marker, String message, Object arg1, Object arg2) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(marker, message, arg1, arg2));
        }
    }

    @Override
    public void error(Marker marker, String message, Object... args) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(marker, message, args));
        }
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        if (delegateLogger.isErrorEnabled()) {
            withLoggingContext(() -> delegateLogger.error(marker, message, throwable));
        }
    }
}
