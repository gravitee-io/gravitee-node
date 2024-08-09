/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.container;

/**
 *
 * Interface to implement by the products to identify uniquely who they are. It allows self-awareness at the highest level as it inherits from {@link io.gravitee.node.container.ContainerInitializer}       .
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface GraviteeProductInitializer extends ContainerInitializer {
    /**
     * Identify gravitee product name (apim-gateway, am-management...). It must be unique across all Gravitee products.
     * @return the unique product name
     */
    String productName();
}
