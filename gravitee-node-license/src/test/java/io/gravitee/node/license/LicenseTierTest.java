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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.license.model.LicenseTier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class LicenseTierTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void should_have_no_attributes_when_tier_declares_none() {
        LicenseTier tier = OBJECT_MAPPER.convertValue(Map.of("packs", List.of("enterprise")), LicenseTier.class);

        assertThat(tier.getAttributes()).isEmpty();
    }

    @Test
    void should_have_no_attributes_when_tier_declares_an_empty_attributes_key() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("packs", List.of("enterprise"));
        raw.put("attributes", null);

        LicenseTier tier = OBJECT_MAPPER.convertValue(raw, LicenseTier.class);

        assertThat(tier.getAttributes()).isEmpty();
    }
}
