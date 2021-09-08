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
package io.gravitee.node.management.http.metrics.prometheus;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.backends.BackendRegistries;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PrometheusEndpoint implements ManagementEndpoint {

  @Override
  public HttpMethod method() {
    return HttpMethod.GET;
  }

  @Override
  public String path() {
    return "/metrics/prometheus";
  }

  @Override
  public void handle(RoutingContext routingContext) {
    String response =
      ((PrometheusMeterRegistry) BackendRegistries.getDefaultNow()).scrape();
    routingContext.response().end(response);
  }
}
