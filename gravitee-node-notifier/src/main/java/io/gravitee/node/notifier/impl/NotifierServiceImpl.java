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
package io.gravitee.node.notifier.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.notifier.NotifierService;
import io.gravitee.notifier.api.Notification;
import io.gravitee.notifier.api.Notifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotifierServiceImpl extends AbstractService implements NotifierService {

    private final Collection<Notifier> notifiers = new ArrayList<>();

    @Override
    public void register(final Notifier notifier) {
        notifiers.add(notifier);
    }

    @Override
    public CompletableFuture<Void> send(final Notification notification, final Map<String, Object> parameters) {
        final CompletableFuture[] futures = new CompletableFuture[notifiers.size()];
        int index = 0;
        for (final Notifier notifier : notifiers) {
            futures[index++] = notifier.send(notification, parameters);
        }
        return CompletableFuture.allOf(futures);
    }

    @Override
    protected String name() {
        return "Notifier service";
    }
}
