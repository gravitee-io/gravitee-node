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
package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.license.License;
import io.gravitee.node.license.license3j.License3J;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class DefaultLicenseTest {

    private static final Map<String, Object> EFFECTIVE_ATTRIBUTES = Map.of("apim-portals-max", 10);

    private final License3J license3J = mock(License3J.class);
    private final License cut = DefaultLicense.builder()
        .referenceType(License.REFERENCE_TYPE_PLATFORM)
        .referenceId(License.REFERENCE_ID_PLATFORM)
        .effectiveAttributes(EFFECTIVE_ATTRIBUTES)
        .license3j(license3J)
        .build();

    @Test
    void should_return_effective_attributes_when_license_is_valid() {
        when(license3J.isValid()).thenReturn(true);
        when(license3J.isExpired()).thenReturn(false);

        assertThat(cut.getEffectiveAttributes()).isEqualTo(EFFECTIVE_ATTRIBUTES);
    }

    @Test
    void should_return_no_effective_attributes_when_license_is_not_valid() {
        when(license3J.isValid()).thenReturn(false);

        assertThat(cut.getEffectiveAttributes()).isEmpty();
    }
}
