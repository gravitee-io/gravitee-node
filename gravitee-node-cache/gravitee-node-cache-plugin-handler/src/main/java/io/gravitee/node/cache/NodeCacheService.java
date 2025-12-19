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
package io.gravitee.node.cache;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cache.CacheManager;
import lombok.CustomLog;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class NodeCacheService extends AbstractService<NodeCacheService> {

    @Autowired
    @Lazy
    private CacheManager cacheManager;

    @Override
    public void doStart() throws Exception {
        super.doStart();
        try {
            cacheManager.start();
        } catch (NoSuchBeanDefinitionException e) {
            log.error("No Cache manager has been registered.");
            throw new NoCacheManagerException();
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (cacheManager != null) {
            cacheManager.stop();
        }
    }
}
