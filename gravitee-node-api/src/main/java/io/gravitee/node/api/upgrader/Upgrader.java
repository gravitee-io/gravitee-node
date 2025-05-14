/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.api.upgrader;

/**
 * An upgrader runs only once to populate or modify data in the database
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Upgrader {
    /**
     * Executes the upgrade logic for this component.
     * <p>
     * This method is called during the node startup process. It should return {@code true}
     * if the upgrade was successfully applied, or {@code false} if the upgrade is not applicable
     * (e.g., already applied or due to a business condition).
     * <p>
     * Implementations should wrap their logic using {@link #wrapException(ThrowingSupplier)}
     * to ensure consistent exception handling and avoid silent failures.
     * @return true if the upgrade has been successfully applied, false otherwise.
     * @throws UpgraderException if the upgrade failed because of an exception.
     */
    boolean upgrade() throws UpgraderException;

    int getOrder();

    default String version() {
        return null;
    }

    default String identifier() {
        String className = getClass().getName();
        if (version() != null) {
            return className + "_" + version();
        }
        return className;
    }

    /**
     * Wraps the execution of a supplier that may throw an exception, and converts any thrown exception
     * into an {@link UpgraderException}. This method is intended to standardize error handling within
     * upgrade logic.
     * <p>
     * If the supplier returns {@code null}, the method will return {@code false}.
     *
     * @param supplier the logic to execute, typically containing the business code of the upgrade
     * @return {@code true} if the supplier returned {@code true}, {@code false} otherwise (including null/void)
     * @throws UpgraderException if the supplier throws any exception during execution
     */
    default boolean wrapException(ThrowingSupplier<Boolean> supplier) throws UpgraderException {
        try {
            Boolean result = supplier.get();
            return result != null && result;
        } catch (Exception e) {
            throw new UpgraderException(e);
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
