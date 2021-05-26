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
package io.gravitee.node.management.http.vertx.endpoint;

import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.gravitee.node.management.http.endpoint.ManagementEndpointManager;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ManagementEndpointManagerImpl implements ManagementEndpointManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementEndpointManagerImpl.class);

    @Autowired
    @Qualifier("managementRouter")
    private Router nodeRouter;

    @Autowired
    @Qualifier("managementWebhookRouter")
    private Router nodeWebhookRouter;

    @Override
    public void register(ManagementEndpoint endpoint) {
        LOGGER.info("Register a new endpoint for Management API: {} {} [{}]", endpoint.method(), endpoint.path(), endpoint.getClass().getName());

        if (endpoint.isWebhook()) {
            nodeWebhookRouter.route(convert(endpoint.method()), endpoint.path()).handler(endpoint::handle);
        } else {
            nodeRouter.route(convert(endpoint.method()), endpoint.path()).handler(endpoint::handle);
        }
    }

    private HttpMethod convert(io.gravitee.common.http.HttpMethod httpMethod) {
        switch (httpMethod) {
            case CONNECT:
                return HttpMethod.CONNECT;
            case DELETE:
                return HttpMethod.DELETE;
            case GET:
                return HttpMethod.GET;
            case HEAD:
                return HttpMethod.HEAD;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case PATCH:
                return HttpMethod.PATCH;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case TRACE:
                return HttpMethod.TRACE;
            case OTHER:
                return HttpMethod.valueOf(httpMethod.name());
        }

        return null;
    }
}
