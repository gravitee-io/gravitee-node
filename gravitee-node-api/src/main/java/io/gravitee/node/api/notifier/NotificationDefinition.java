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
package io.gravitee.node.api.notifier;

import java.util.Map;
import java.util.Objects;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotificationDefinition {

    /**
     * Resource concerned by the definition
     */
    private String resourceId;
    /**
     * Resource Type concerned by the definition
     */
    private String resourceType;
    /**
     * UserId targeted by the notification
     */
    private String audienceId;
    /**
     * Plugin Notifier configuration
     */
    private String configuration;
    /**
     * Type of notifier (plugin id)
     */
    private String type;
    /**
     * Cron expression used to evaluate the rule
     */
    private String cron;
    /**
     * Context used to evaluate the rule
     */
    private Map<String, Object> data;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAudienceId() {
        return audienceId;
    }

    public void setAudienceId(String audienceId) {
        this.audienceId = audienceId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationDefinition)) return false;
        NotificationDefinition that = (NotificationDefinition) o;
        return (
            Objects.equals(resourceId, that.resourceId) &&
            Objects.equals(resourceType, that.resourceType) &&
            Objects.equals(audienceId, that.audienceId) &&
            Objects.equals(type, that.type)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceId, resourceType, audienceId, type);
    }
}
