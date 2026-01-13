package io.gravitee.node.opentelemetry.tracer.vertx;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
import io.smallrye.common.constraint.Assert;
import io.smallrye.common.constraint.Nullable;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;

public class VertxContext {

    private VertxContext() {}

    public static Context getOrCreateDuplicatedContext(Context context) {
        return (Context) (isDuplicatedContext(context) ? context : ((ContextInternal) context).duplicate());
    }

    public static Context getOrCreateDuplicatedContext(Vertx vertx) {
        Assert.checkNotNullParam("vertx", vertx);
        Context context = vertx.getOrCreateContext();
        return (Context) (isDuplicatedContext(context) ? context : ((ContextInternal) context).duplicate());
    }

    @Nullable
    public static Context getOrCreateDuplicatedContext() {
        Context context = Vertx.currentContext();
        return context == null ? null : getOrCreateDuplicatedContext(context);
    }

    @Nullable
    public static Context createNewDuplicatedContext() {
        return createNewDuplicatedContext(Vertx.currentContext());
    }

    @Nullable
    public static Context createNewDuplicatedContext(Context context) {
        return context == null ? null : ((ContextInternal) context).duplicate();
    }

    public static boolean isDuplicatedContext(Context context) {
        ContextInternal actual = (ContextInternal) Assert.checkNotNullParam("context", (ContextInternal) context);
        return actual.isDuplicate();
    }

    public static boolean isOnDuplicatedContext() {
        Context context = Vertx.currentContext();
        return context != null && isDuplicatedContext(context);
    }

    public static Context getRootContext(Context context) {
        return (Context) (isDuplicatedContext(context) ? ((ContextInternal) context).unwrap() : context);
    }
}
