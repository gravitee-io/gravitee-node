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

import java.io.Serializable;
import java.util.Map;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheck implements Serializable {

    /**
     * Flag indicating if the global status of health check is healthy or not.
     * It is basically based on the healthiness of all the health check probes.
     */
    private boolean isHealthy;

    /**
     * The date on which the health check has been performed.
     */
    private long evaluatedAt;

    /**
     * Map of probe and associated result.
     */
    private Map<String, Result> results;

    public HealthCheck() {
        super();
    }

    public HealthCheck(long evaluatedAt, Map<String, Result> results) {
        this.evaluatedAt = evaluatedAt;
        this.results = results;
        this.isHealthy = this.results.values().stream().allMatch(Result::isHealthy);
    }

    public Map<String, Result> getResults() {
        return results;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public void setResults(Map<String, Result> results) {
        this.results = results;
    }

    public long getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(long evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
