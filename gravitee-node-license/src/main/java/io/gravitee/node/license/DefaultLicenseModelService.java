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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.node.api.license.LicenseModelService;
import io.gravitee.node.api.license.model.LicenseModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultLicenseModelService implements LicenseModelService {

    private static final String YAML_MODEL = "license-model.yml";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LicenseModel licenseModel;

    @Override
    public LicenseModel getLicenseModel() {
        if (licenseModel == null) {
            licenseModel = loadModel();
        }
        return licenseModel;
    }

    private static LicenseModel loadModel() {
        try (InputStream stream = LicenseModel.class.getClassLoader().getResourceAsStream(YAML_MODEL)) {
            // SnakeYAML is used here (rather than Jackson's YAMLMapper) because it fully resolves YAML
            // anchors/aliases, even when they reference sequences. This allows a tier to extend another one
            // by referencing its packs (and, similarly, a pack to extend another by referencing its features), e.g.:
            //     planet:
            //         packs: &planet-packs
            //             - pack-a
            //     galaxy:
            //         packs:
            //             - *planet-packs
            //             - pack-b
            Map<String, Object> raw = new Yaml().load(stream);
            flattenPackFeatures(raw);
            flattenTierPacks(raw);
            return OBJECT_MAPPER.convertValue(raw, LicenseModel.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load license model", e);
        }
    }

    /**
     * Referencing a sequence anchor as a list item (e.g. {@code - *planet-packs}) produces a nested list.
     * This flattens each tier's packs back into a single-level list so a tier can transparently extend another.
     */
    private static void flattenTierPacks(Map<String, Object> raw) {
        flattenNestedSequences(raw, "tiers", "packs");
    }

    /**
     * Same as {@link #flattenTierPacks(Map)} but for packs: flattens each pack's features so a pack can
     * transparently extend another one by referencing its features via a YAML anchor.
     */
    private static void flattenPackFeatures(Map<String, Object> raw) {
        flattenNestedSequences(raw, "packs", "features");
    }

    @SuppressWarnings("unchecked")
    private static void flattenNestedSequences(Map<String, Object> raw, String sectionKey, String listKey) {
        Object section = raw == null ? null : raw.get(sectionKey);
        if (!(section instanceof Map)) {
            return;
        }
        for (Object entry : ((Map<String, Object>) section).values()) {
            if (entry instanceof Map) {
                Map<String, Object> entryMap = (Map<String, Object>) entry;
                Object list = entryMap.get(listKey);
                if (list instanceof List) {
                    entryMap.put(listKey, flatten((List<Object>) list));
                }
            }
        }
    }

    private static List<Object> flatten(List<Object> values) {
        List<Object> flattened = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof List) {
                flattened.addAll(flatten((List<Object>) value));
            } else {
                flattened.add(value);
            }
        }
        return flattened;
    }
}
