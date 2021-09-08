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
package io.gravitee.node.plugins.service.impl;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.plugins.service.ServiceManager;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ServiceManagerImpl
  extends AbstractService
  implements ServiceManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ServiceManagerImpl.class
  );

  private final List<AbstractService> services = new ArrayList<>();

  @Override
  public void register(AbstractService service) {
    services.add(service);
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();

    for (AbstractService service : services) {
      try {
        service.start();
      } catch (Exception ex) {
        LOGGER.error("Unexpected error while starting service", ex);
      }
    }
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();

    for (AbstractService service : services) {
      try {
        service.stop();
      } catch (Exception ex) {
        LOGGER.error("Unexpected error while stopping service", ex);
      }
    }
  }

  @Override
  protected String name() {
    return "Plugins - Services Manager";
  }
}
