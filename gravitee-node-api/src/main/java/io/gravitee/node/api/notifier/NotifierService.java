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

import io.reactivex.Completable;

/**
 * This service manages notifications at platform level.
 *
 * When a notification is registered a definition and notification conditions are required.
 *
 * The definition will describe the resource on which the notification is created (certificate, API Key...), the notification channel and the audience.
 * In addition of these basic information, the definition also describe the frequency on which the notification rules have to be evaluated and the configuration used to instantiate the notifier plugin.
 *
 * Once a notification has been sent, an acknowledgement is created into database to monitor the number of notification sent for a given resource to a given audience.
 *
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface NotifierService {
    /**
     * Register a notification in memory.
     *
     * @param definition Bean that contains all the settings to instantiate the notifier
     * @param condition Rule evaluate to determine if the notification has to be triggered for the first time
     * @param resendCondition Rule evaluate to determine if the notification has to be triggered another time
     */
    void register(NotificationDefinition definition, NotificationCondition condition, ResendNotificationCondition resendCondition);

    /**
     * Remove the notification trigger identified by the given parameters
     *
     * @param resourceId resourceId for which the notification has been created
     * @param resourceType the type of resource
     * @param type the type of notification (ex: email, mebhook...)
     * @param audienceId the identifier of the notification audience.
     */
    void unregister(String resourceId, String resourceType, String type, String audienceId);

    /**
     * Remove all the triggers for a given resource
     *
     * @param resourceId resourceId for which the notification has been created
     * @param resourceType the type of resource
     */
    void unregisterAll(String resourceId, String resourceType);

    /**
     * Remove all acknowledge entries for the given resource.
     * @param resourceId
     * @param resourceType
     * @return
     */
    Completable deleteAcknowledge(String resourceId, String resourceType);
}
