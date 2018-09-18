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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NotifierServiceImpl extends AbstractService implements NotifierService {

    private final Logger logger = LoggerFactory.getLogger(NotifierServiceImpl.class);

    private final Collection<Notifier> notifiers = new ArrayList<>();

    @Override
    public void register(final Notifier notifier) {
        notifiers.add(notifier);
    }

    @Override
    public void send(final Notification notification, final Map<String, Object> parameters) {
        for (final Notifier notifierService : notifiers) {
            if (notifierService.canHandle(notification)) {
                notifierService.send(notification, parameters);
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        if (!notifiers.isEmpty()) {
            for (final Notifier notifierService : notifiers) {
                try {
                    notifierService.start();
                } catch (Exception ex) {
                    logger.error("Unexpected error while starting notifier", ex);
                }
            }
        } else {
            logger.info("\tThere is no notifier to start");
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        for(final Notifier notifier : notifiers) {
            try {
                notifier.stop();
            } catch (Exception ex) {
                logger.error("Unexpected error while stopping notifier", ex);
            }
        }
    }

    @Override
    protected String name() {
        return "Notifier service";
    }
}
