package io.gravitee.node.api.secrets.resolver;

import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface WatchablePropertyResolver<T> extends PropertyResolver<T> {
    /**
     * Check if this property can be watched
     *
     * @param value the property value
     * @return true if {@link #watch(String)} can be called
     */
    default boolean isWatchable(String value) {
        return true;
    }

    /**
     * Watch for any changes in the property and emit the new values
     *
     * @param location the value as a URL
     * @return a Flowable of resolved value
     */
    Flowable<T> watch(String location);
}
