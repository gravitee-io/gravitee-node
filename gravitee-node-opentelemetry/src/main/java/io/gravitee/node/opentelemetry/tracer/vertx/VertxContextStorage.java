package io.gravitee.node.opentelemetry.tracer.vertx;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;
import lombok.CustomLog;

/**
 * See <a href="https://github.com/quarkusio/quarkus/blob/main/extensions/opentelemetry/runtime/src/main/java/io/quarkus/opentelemetry/runtime/QuarkusContextStorage.java">QuarkusContextStorage.java</a>
 */
@CustomLog
public enum VertxContextStorage implements ContextStorage {
    INSTANCE;

    private static final String OTEL_CONTEXT = VertxContextStorage.class.getName() + ".otelContext";

    /**
     * Attach the OpenTelemetry Context to the current Context. If a Vert.x Context is available, and it is a duplicated
     * Vert.x Context the OpenTelemetry Context is attached to the Vert.x Context. Otherwise, fallback to the
     * OpenTelemetry default ContextStorage.
     *
     * @param toAttach the OpenTelemetry Context to attach
     * @return the Scope of the OpenTelemetry Context
     */
    @Override
    public Scope attach(Context toAttach) {
        io.vertx.core.Context vertxContext = getVertxContext();
        return vertxContext != null && VertxContext.isDuplicatedContext(vertxContext)
            ? attach(vertxContext, toAttach)
            : ContextStorage.defaultStorage().attach(toAttach);
    }

    /**
     * Attach the OpenTelemetry Context in the Vert.x Context if it is a duplicated Vert.x Context.
     *
     * @param vertxContext the Vert.x Context to attach the OpenTelemetry Context
     * @param toAttach the OpenTelemetry Context to attach
     * @return the Scope of the OpenTelemetry Context
     */
    public Scope attach(io.vertx.core.Context vertxContext, Context toAttach) {
        if (vertxContext == null || toAttach == null) {
            return Scope.noop();
        }

        // We don't allow to attach the OpenTelemetry Context to a Vert.x Context that is not a duplicate.
        if (!VertxContext.isDuplicatedContext(vertxContext)) {
            throw new IllegalArgumentException("The Vert.x Context to attach the OpenTelemetry Context must be a duplicated Context");
        }

        Context beforeAttach = getContext(vertxContext);
        if (toAttach == beforeAttach) {
            return Scope.noop();
        }
        vertxContext.putLocal(OTEL_CONTEXT, toAttach);

        return () -> {
            if (beforeAttach == null) {
                vertxContext.removeLocal(OTEL_CONTEXT);
            } else {
                vertxContext.putLocal(OTEL_CONTEXT, beforeAttach);
            }
        };
    }

    /**
     * Gets the current OpenTelemetry Context from the current Vert.x Context if one exists or from the default
     * ContextStorage. The current Vert.x Context must be a duplicated Context.
     *
     * @return the current OpenTelemetry Context or null.
     */
    @Override
    public Context current() {
        io.vertx.core.Context current = getVertxContext();
        if (current != null) {
            return current.getLocal(OTEL_CONTEXT);
        } else {
            return ContextStorage.defaultStorage().current();
        }
    }

    /**
     * Gets the OpenTelemetry Context in a Vert.x Context. The Vert.x Context has to be a duplicate context.
     *
     * @param vertxContext a Vert.x Context.
     * @return the OpenTelemetry Context if exists in the Vert.x Context or null.
     */
    public static Context getContext(io.vertx.core.Context vertxContext) {
        return vertxContext != null && VertxContext.isDuplicatedContext(vertxContext) ? vertxContext.getLocal(OTEL_CONTEXT) : null;
    }

    /**
     * Gets the current duplicated context or a new duplicated context if a Vert.x Context exists. Multiple invocations
     * of this method may return the same or different context. If the current context is a duplicate one, multiple
     * invocations always return the same context. If the current context is not duplicated, a new instance is returned
     * with each method invocation.
     *
     * @return a duplicated Vert.x Context or null.
     */
    public static io.vertx.core.Context getVertxContext() {
        io.vertx.core.Context context = Vertx.currentContext();
        if (context != null && VertxContext.isOnDuplicatedContext()) {
            return context;
        } else if (context != null) {
            return VertxContext.createNewDuplicatedContext(context);
        }
        return null;
    }
}
