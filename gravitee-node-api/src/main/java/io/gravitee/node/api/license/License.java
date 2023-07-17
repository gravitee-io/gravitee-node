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
package io.gravitee.node.api.license;

import java.util.Map;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface License {
    Optional<Feature> feature(String name);

    Map<String, Object> features();

    /**
     * @deprecated since 4.0.0.
     * This method is kept because Alert Engine as not been updated to the new license model and still uses
     * the old way of checking the alert-engine feature.
     */
    @Deprecated(since = "4.0.0", forRemoval = true)
    boolean isFeatureIncluded(String name);
}
