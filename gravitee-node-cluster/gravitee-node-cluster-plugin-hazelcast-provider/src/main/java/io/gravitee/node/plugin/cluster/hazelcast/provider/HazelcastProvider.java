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
package io.gravitee.node.plugin.cluster.hazelcast.provider;

/**
 * Marker for the {@code hazelcast-provider} plugin. The plugin framework uses this class as the
 * target of {@code class=} in {@code plugin.properties}. All actual wiring happens via the Spring
 * {@code @Configuration} loaded alongside the plugin context.
 */
public class HazelcastProvider {}
