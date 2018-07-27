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
package io.gravitee.node.service.healthcheck;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.gravitee.node.service.healthcheck.management.HealthcheckManagementEndpoint;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeHealthcheckService extends AbstractService {

    @Autowired
    private ManagementEndpointManager managementEndpointManager;

    @Autowired
    private ProbesLoader probesLoader;

    @Autowired
    private HealthcheckManagementEndpoint healthcheckEndpoint;

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        managementEndpointManager.register(healthcheckEndpoint);
        healthcheckEndpoint.setProbes(probesLoader.getProbes());
    }

    @Override
    protected String name() {
        return "Node Health-check service";
    }
}
