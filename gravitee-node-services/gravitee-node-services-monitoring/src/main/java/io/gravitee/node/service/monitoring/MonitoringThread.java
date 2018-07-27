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
package io.gravitee.node.service.monitoring;

import io.gravitee.node.api.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MonitoringThread implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringThread.class);

    //@Autowired
    //private ReporterService reporterService;

    @Autowired
    private Node node;

    @Override
    public void run() {
        try {
            // And generate monitoring metrics
            /*
            reporterService.report(
                    Monitor
                    .on(node.id())
                    .at(System.currentTimeMillis())
                    .os(OsProbe.getInstance().osInfo())
                    .jvm(JvmProbe.getInstance().jvmInfo())
                    .process(ProcessProbe.getInstance().processInfo())
                    .build());
            */
        } catch (Exception ex) {
            LOGGER.error("Unexpected error occurs while monitoring the node", ex);
        }
    }
}