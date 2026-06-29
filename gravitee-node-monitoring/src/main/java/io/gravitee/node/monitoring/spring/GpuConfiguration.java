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
package io.gravitee.node.monitoring.spring;

import java.util.concurrent.TimeUnit;

/**
 * Configuration of the GPU monitoring refresh rate. By default it inherits the node monitoring
 * interval ({@code services.monitoring.delay}/{@code unit}) but can be overridden independently via
 * {@code services.monitoring.gpu.*} to probe the GPU at a different pace.
 *
 * @author GraviteeSource Team
 */
public record GpuConfiguration(boolean enabled, long delay, TimeUnit unit) {}
