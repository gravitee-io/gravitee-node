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
package io.gravitee.node.monitoring.monitor;

import static io.gravitee.node.monitoring.MonitoringConstants.*;

import io.gravitee.alert.api.event.DefaultEvent;
import io.gravitee.alert.api.event.Event;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.monitor.JvmInfo;
import io.gravitee.node.api.monitor.Monitor;
import io.gravitee.node.api.monitor.OsInfo;
import io.gravitee.node.api.monitor.ProcessInfo;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.monitor.probe.OsProbe;
import io.gravitee.node.monitoring.monitor.probe.ProcessProbe;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.vertx.core.eventbus.MessageProducer;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class NodeMonitorThread implements Runnable {

    private final MessageProducer<Monitor> producer;
    private final Node node;
    private final AlertEventProducer alertEventProducer;

    @Override
    public void run() {
        try {
            Monitor monitor = Monitor
                .on(node.id())
                .at(System.currentTimeMillis())
                .os(OsProbe.getInstance().osInfo())
                .jvm(JvmProbe.getInstance().jvmInfo())
                .process(ProcessProbe.getInstance().processInfo())
                .build();

            // And generate monitoring metrics
            producer.write(monitor);

            if (!alertEventProducer.isEmpty()) {
                DefaultEvent.Builder event = Event.at(monitor.getTimestamp()).type(NODE_HEARTBEAT);

                event.property(PROPERTY_NODE_ID, node.id());
                event.property(PROPERTY_NODE_HOSTNAME, node.hostname());
                event.property(PROPERTY_NODE_APPLICATION, node.application());
                event.organizations((Set<String>) node.metadata().get(Node.META_ORGANIZATIONS));
                event.environments((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS));

                // OS metrics
                OsInfo osInfo = monitor.getOs();
                event.property("os.cpu.percent", osInfo.cpu.getPercent());
                if (osInfo.cpu.getLoadAverage() != null) {
                    for (int i = 0; i < osInfo.cpu.getLoadAverage().length; i++) {
                        event.property("os.cpu.average." + i, osInfo.cpu.getLoadAverage()[i]);
                    }
                }

                // Process metrics
                ProcessInfo processInfo = monitor.getProcess();
                event.property("process.fd.open", processInfo.openFileDescriptors);
                event.property("process.fd.max", processInfo.maxFileDescriptors);
                event.property("process.cpu.percent", processInfo.cpu.percent);
                event.property("process.cpu.total", processInfo.cpu.total);
                event.property("process.mem.virtual.total", processInfo.mem.totalVirtual);

                // JVM metrics
                JvmInfo jvmInfo = monitor.getJvm();
                event.property("jvm.uptime", jvmInfo.uptime);
                event.property("jvm.threads.count", jvmInfo.threads.count);
                event.property("jvm.threads.peak", jvmInfo.threads.peakCount);
                event.property("jvm.mem.heap.used", jvmInfo.mem.heapUsed);
                event.property("jvm.mem.heap.max", jvmInfo.mem.heapMax);
                event.property("jvm.mem.heap.percent", jvmInfo.mem.getHeapUsedPercent());

                alertEventProducer.send(event.build());
            }
        } catch (Exception ex) {
            log.error("Unexpected error occurs while monitoring the node", ex);
        }
    }
}
