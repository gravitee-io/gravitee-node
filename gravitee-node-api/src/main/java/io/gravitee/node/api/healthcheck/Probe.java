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
package io.gravitee.node.api.healthcheck;

import java.util.concurrent.CompletionStage;

/**
 * Represents a probe that can be evaluated and used for monitoring purpose.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Probe {
    /**
     * The identifier of the probe (ex: 'cpu', 'memory', ...).
     * @return
     */
    String id();

    /**
     * Evaluate the probe and return a result indicating if it is healthy or not.
     *
     * @return a {@link CompletionStage} containing the result of the evaluation.
     */
    CompletionStage<Result> check();

    /**
     * Indicates if the probe requests caching to avoid too many evaluations that can have an impact on performances.
     * Note: the probe itself is not mandatory to implement caching. This is the responsibility of the invoker to implement the caching of the result.
     *
     * @return <code>true</code> if the probe requests caching, <code>false</code> otherwise. Default is <code>false</code>.
     */
    default boolean isCacheable() {
        return false;
    }

    /**
     * Indicates if the probe should be evaluated and visible by default by the health check.
     *
     * @return <code>true</code> if the probe should be visible, <code>false</code> otherwise. Default is <code>true</code>.
     */
    default boolean isVisibleByDefault() {
        return true;
    }
}
