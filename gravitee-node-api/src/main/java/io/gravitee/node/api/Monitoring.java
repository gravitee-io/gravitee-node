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
package io.gravitee.node.api;

import java.util.Date;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Monitoring {

    public static final String HEALTH_CHECK = "HEALTH_CHECK";
    public static final String MONITOR = "MONITOR";
    public static final String NODE_INFOS = "NODE_INFOS";

    /**
     * The unique identifier of the monitoring object.
     */
    private String id;

    /**
     * The identifier of the node the monitoring object depends on.
     */
    private String nodeId;

    /**
     * The type of monitoring object.
     */
    private String type;

    /**
     * The payload data.
     * @see io.gravitee.node.api.healthcheck.HealthCheck
     * @see io.gravitee.node.api.monitor.Monitor
     * @see io.gravitee.node.api.infos.NodeInfos
     */
    private String payload;

    /**
     * The creation date.
     */
    private Date createdAt;

    /**
     * The date corresponding to the time where the monitoring data have been evaluated.
     */
    private Date evaluatedAt;

    /**
     * The last update date.
     */
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Date evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}
