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
package io.gravitee.node.monitoring.healthcheck;

import io.gravitee.common.spring.factory.SpringFactoriesLoader;
import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.ProbeManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ProbeManagerImpl extends SpringFactoriesLoader<Probe> implements ProbeManager {

    @Override
    protected Class<Probe> getObjectType() {
        return Probe.class;
    }

    private Map<String, Probe> probes = new HashMap<>();

    @Override
    public List<Probe> getProbes() {
        ArrayList<Probe> probes = new ArrayList<>(getFactoriesInstances());
        probes.addAll(this.probes.values());
        return probes;
    }

    @Override
    public void register(Probe probe) {
        probes.put(probe.id(), probe);
    }

    @Override
    public void unregister(Probe probe) {
        if (probes.containsKey(probe.id())) {
            probes.remove(probe.id());
        }
    }
}
