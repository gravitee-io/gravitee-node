package io.gravitee.node.logging;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.node.api.Node;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory class for creating and managing loggers that are aware of a {@link Node} instance.
 * This factory can provide standard loggers or loggers that include contextual information from a {@link Node}.
 *
 * This class is designed to be used in environments where a Node is central to the application's context,
 * and it allows developers to integrate the Node's details into the logging system.
 *
 * The {@link NodeLoggerFactory} operates as a singleton, storing a single {@link Node} instance or a supplier
 * that provides a {@link Node}. It is thread-safe and ensures that every logger generated can access the
 * contextual information if a Node is available.
 */
public final class NodeLoggerFactory {

    private static final AtomicReference<Supplier<Node>> NODE_SUPPLIER = new AtomicReference<>();
    private static final AtomicReference<MdcLoggingConfiguration> MDC_CONFIGURATION = new AtomicReference<>();

    private NodeLoggerFactory() {}

    /**
     * Initializes the node context for the {@link NodeLoggerFactory}.
     * This method associates a specific {@link Node} instance with the logging system.
     * Once initialized, loggers created through {@link NodeLoggerFactory} will have access to the contextual
     * information provided by the specified {@link Node}.
     *
     * @param node the {@link Node} instance to be set in the logging context. Must not be null.
     * @throws NullPointerException if the provided {@link Node} is null.
     */
    public static void init(Node node) {
        Objects.requireNonNull(node, "node");
        NODE_SUPPLIER.set(() -> node);
    }

    /**
     * Initializes the {@link NodeLoggerFactory} with a supplier that provides {@link Node} instances.
     * This allows the logging system to dynamically retrieve a {@link Node} at runtime, enabling
     * loggers to include contextual information from the supplied {@link Node}.
     *
     * @param nodeSupplier a {@link Supplier} providing {@link Node} instances. Must not be null.
     * @throws NullPointerException if the provided {@link Supplier} is null.
     */
    public static void init(Supplier<Node> nodeSupplier) {
        Objects.requireNonNull(nodeSupplier, "nodeSupplier");
        NODE_SUPPLIER.set(nodeSupplier);
    }

    /**
     * Initializes the MDC logging configuration.
     * Once set, all loggers created through this factory will filter and format MDC entries
     * according to the provided configuration.
     *
     * @param configuration the {@link MdcLoggingConfiguration} to apply. Must not be null.
     * @throws NullPointerException if the provided configuration is null.
     */
    public static void initMdcConfiguration(MdcLoggingConfiguration configuration) {
        Objects.requireNonNull(configuration, "mdcLoggingConfiguration");
        MDC_CONFIGURATION.set(configuration);
    }

    /**
     * Returns the current {@link MdcLoggingConfiguration}, or {@code null} if not yet initialized.
     * Used by logback converters (e.g., {@code %mdcList}) to access the MDC formatting configuration.
     *
     * @return the current MDC logging configuration, or {@code null}
     */
    public static MdcLoggingConfiguration getMdcConfiguration() {
        return MDC_CONFIGURATION.get();
    }

    /**
     * Resets the MDC configuration to its uninitialized state. Intended for testing only.
     */
    @VisibleForTesting
    public static void resetMdcConfiguration() {
        MDC_CONFIGURATION.set(null);
    }

    public static Logger getLogger(Class<?> type) {
        // Always return a NodeAwareLogger, it will handle by itself the absence or presence of Node
        return new NodeAwareLogger(NODE_SUPPLIER, MDC_CONFIGURATION, LoggerFactory.getLogger(type));
    }

    public static Logger getLogger(String name) {
        // Always return a NodeAwareLogger, it will handle by itself the absence or presence of Node
        return new NodeAwareLogger(NODE_SUPPLIER, MDC_CONFIGURATION, LoggerFactory.getLogger(name));
    }
}
