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
package io.gravitee.node.monitoring;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MonitoringConstants {
  String PROPERTY_NODE_ID = "node.id";
  String PROPERTY_NODE_HOSTNAME = "node.hostname";
  String PROPERTY_NODE_APPLICATION = "node.application";
  String PROPERTY_NODE_EVENT = "node.event";
  String NODE_HEALTHCHECK = "NODE_HEALTHCHECK";
  String NODE_HEARTBEAT = "NODE_HEARTBEAT";
  String NODE_LIFECYCLE = "NODE_LIFECYCLE";
  String NODE_EVENT_START = "NODE_START";
  String NODE_EVENT_STOP = "NODE_STOP";
  String PROPERTY_NODE_HEALTHY = "node.healthy";
  String PROPERTY_PROBE_SUFFIX = "node.probe.";
}
