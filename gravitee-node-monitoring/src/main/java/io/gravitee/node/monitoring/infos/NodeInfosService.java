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
package io.gravitee.node.monitoring.infos;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.util.Version;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.infos.NodeInfos;
import io.gravitee.node.api.infos.NodeStatus;
import io.gravitee.node.api.infos.PluginInfos;
import io.gravitee.node.monitoring.eventbus.NodeInfosCodec;
import io.gravitee.node.monitoring.monitor.probe.Constants;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.tracing.TracingPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeInfosService extends AbstractService<NodeInfosService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeInfosService.class);
    public static final String GIO_NODE_INFOS_BUS = "gio:node:infos";

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private Environment environment;

    @Autowired
    private Node node;

    @Autowired
    private Vertx vertx;

    private MessageProducer<NodeInfos> messageProducer;

    private NodeInfos nodeInfos;

    @Override
    protected void doStart() throws Exception {
        LOGGER.info("Starting node infos service");

        super.doStart();

        messageProducer =
            vertx
                .eventBus()
                .registerCodec(new NodeInfosCodec())
                .sender(
                    GIO_NODE_INFOS_BUS,
                    new DeliveryOptions().setCodecName(NodeInfosCodec.CODEC_NAME).setTracingPolicy(TracingPolicy.IGNORE)
                );

        nodeInfos = buildNodeInfos();
        nodeInfos.setStatus(NodeStatus.STARTED);
        messageProducer.write(nodeInfos);

        LOGGER.info("Start node infos service: DONE");
    }

    public NodeInfosService preStop() {
        nodeInfos.setStatus(NodeStatus.STOPPED);
        messageProducer.write(nodeInfos);

        return this;
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("Stopping node infos service");

        super.doStop();

        LOGGER.info("Stop node infos service : DONE");
    }

    @Override
    protected String name() {
        return "Node Infos Service";
    }

    private NodeInfos buildNodeInfos() {
        NodeInfos nodeInfos = new NodeInfos();

        nodeInfos.setId(node.id());
        nodeInfos.setName(node.name());
        nodeInfos.setApplication(node.application());
        nodeInfos.setVersion(Version.RUNTIME_VERSION.toString());
        nodeInfos.setTags(getShardingTags());
        nodeInfos.setTenant(getTenant());
        nodeInfos.setPluginInfos(plugins());
        nodeInfos.setJdkVersion(Constants.JVM_NAME + " " + Constants.JVM_VERSION);
        nodeInfos.setEvaluatedAt(System.currentTimeMillis());

        try {
            nodeInfos.setPort(getPort());
        } catch (NumberFormatException nfe) {
            LOGGER.warn("Could not get http server port.", nfe);
        }

        try {
            nodeInfos.setHostname(InetAddress.getLocalHost().getHostName());
            nodeInfos.setIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException uhe) {
            LOGGER.warn("Could not get hostname / IP", uhe);
        }

        return nodeInfos;
    }

    private Set<PluginInfos> plugins() {
        return pluginRegistry
            .plugins()
            .stream()
            .map(
                regPlugin -> {
                    PluginInfos plugin = new PluginInfos();
                    plugin.setId(regPlugin.id());
                    plugin.setName(regPlugin.manifest().name());
                    plugin.setDescription(regPlugin.manifest().description());
                    plugin.setVersion(regPlugin.manifest().version());
                    plugin.setType(regPlugin.type().toLowerCase());
                    plugin.setPlugin(regPlugin.clazz());
                    return plugin;
                }
            )
            .collect(Collectors.toSet());
    }

    private List<String> getShardingTags() {
        return Arrays.asList(environment.getProperty("tags", "").split(","));
    }

    private String getTenant() {
        return environment.getProperty("tenant");
    }

    private String getZone() {
        return environment.getProperty("zone");
    }

    private int getPort() {
        return Integer.parseInt(environment.getProperty("http.port", environment.getProperty("jetty.port", "-1")));
    }
}
