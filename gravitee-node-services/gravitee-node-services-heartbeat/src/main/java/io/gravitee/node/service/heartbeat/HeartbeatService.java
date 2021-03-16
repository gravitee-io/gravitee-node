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
package io.gravitee.node.service.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.heartbeat.Event;
import io.gravitee.node.api.heartbeat.EventType;
import io.gravitee.node.service.heartbeat.event.InstanceEventPayload;
import io.gravitee.node.service.heartbeat.event.Plugin;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeartbeatService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatService.class);

    @Value("${services.heartbeat.enabled:true}")
    private boolean enabled;

    @Value("${services.heartbeat.delay:5000}")
    private int delay;

    @Value("${services.heartbeat.unit:MILLISECONDS}")
    private TimeUnit unit;

    @Value("${services.heartbeat.storeSystemProperties:true}")
    private boolean storeSystemProperties;

    @Autowired
    private PluginRegistry pluginRegistry;

    private Event heartbeatEvent;

    private ExecutorService executorService;

    @Autowired
    private Node node;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Vertx vertx;

    private MessageProducer<Event> producer;

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            heartbeatEvent = prepareEvent();

            executorService = Executors.newSingleThreadScheduledExecutor(
                    r -> new Thread(r, "node-heartbeat"));

            producer = vertx.eventBus().sender("node:heartbeat");
            HeartbeatThread heartbeatThread = new HeartbeatThread(producer, heartbeatEvent);
            this.applicationContext.getAutowireCapableBeanFactory().autowireBean(heartbeatThread);

            LOGGER.info("Node heartbeat scheduled with fixed delay {} {} ", delay, unit.name());

            ((ScheduledExecutorService) executorService).scheduleWithFixedDelay(
                    heartbeatThread, 0, delay, unit);
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            if (! executorService.isShutdown()) {
                LOGGER.info("Stop node heartbeat");
                executorService.shutdownNow();
            } else {
                LOGGER.info("Node heartbeat already shut-downed");
            }

            heartbeatEvent.setType(EventType.NODE_STOPPED);
            producer.write(heartbeatEvent);

            super.doStop();

            LOGGER.info("Stop node heartbeat : DONE");
        }
    }

    @Override
    protected String name() {
        return "Heartbeat Service";
    }

    private Event prepareEvent() {
        Event event = new Event();
        event.setId(node.id());
        event.setType(EventType.NODE_STARTED);
        event.setCreatedAt(new Date());
        event.setUpdatedAt(event.getCreatedAt());

        InstanceEventPayload instance = createInstanceInfo();

        try {
            String payload = objectMapper.writeValueAsString(instance);
            event.setPayload(payload);
        } catch (JsonProcessingException jsex) {
            LOGGER.error("An error occurs while transforming instance information into JSON", jsex);
        }
        return event;
    }

    private InstanceEventPayload createInstanceInfo() {
        InstanceEventPayload instanceInfo = new InstanceEventPayload();

        instanceInfo.setId(node.id());
        instanceInfo.setVersion(Version.RUNTIME_VERSION.toString());

//        Optional<List<String>> shardingTags = gatewayConfiguration.shardingTags();
//        instanceInfo.setTags(shardingTags.orElse(null));

        instanceInfo.setPlugins(plugins());
        instanceInfo.setSystemProperties(getSystemProperties());
//        instanceInfo.setPort(port);

 //       Optional<String> tenant = gatewayConfiguration.tenant();
//        instanceInfo.setTenant(tenant.orElse(null));

        try {
            instanceInfo.setHostname(InetAddress.getLocalHost().getHostName());
            instanceInfo.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException uhe) {
            LOGGER.warn("Could not get hostname / IP", uhe);
        }

        return instanceInfo;
    }

    private Set<Plugin> plugins() {
        return pluginRegistry.plugins().stream().map(regPlugin -> {
            Plugin plugin = new Plugin();
            plugin.setId(regPlugin.id());
            plugin.setName(regPlugin.manifest().name());
            plugin.setDescription(regPlugin.manifest().description());
            plugin.setVersion(regPlugin.manifest().version());
            plugin.setType(regPlugin.type().toLowerCase());
            plugin.setPlugin(regPlugin.clazz());
            return plugin;
        }).collect(Collectors.toSet());
    }

    private Map getSystemProperties() {
        if (storeSystemProperties) {
            return System.getProperties()
                    .entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().toString().toUpperCase().startsWith("GRAVITEE"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return Collections.emptyMap();
    }
}
