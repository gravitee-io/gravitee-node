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

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.node.api.license.LicenseModelService;
import io.gravitee.node.api.license.model.LicenseModel;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class LicenseModelServiceImpl implements LicenseModelService {

    private static final String YAML_MODEL = "license-model.yml";
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

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
            return YAML_MAPPER.readValue(stream, LicenseModel.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load license model", e);
        }
    }
}
