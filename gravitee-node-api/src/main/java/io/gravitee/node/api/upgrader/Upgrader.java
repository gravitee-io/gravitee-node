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
     * Implementations can override this method to state that they ran without
     * committing any change. If the return is true, the upgrade will not be stored
     * and will run again on subsequent executions until the implementation returns false.
     *
     * @return true if the upgrader actually committed its changes, false otherwise.
     */
    default boolean isDryRun() {
        return false;
    }

    boolean upgrade();

    int getOrder();
}
