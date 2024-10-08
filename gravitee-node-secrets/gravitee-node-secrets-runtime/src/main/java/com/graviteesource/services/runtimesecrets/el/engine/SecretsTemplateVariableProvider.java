/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graviteesource.services.runtimesecrets.el.engine;

import com.graviteesource.services.runtimesecrets.el.Service;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.el.annotations.TemplateVariable;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import lombok.RequiredArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@TemplateVariable(scopes = { TemplateVariableScope.API, TemplateVariableScope.HEALTH_CHECK })
public class SecretsTemplateVariableProvider implements TemplateVariableProvider {

    private final Cache cache;
    private final GrantService grantService;
    private final SpecLifecycleService specLifecycleService;
    private final SpecRegistry specRegistry;

    @Override
    public void provide(TemplateContext context) {
        context.setVariable("secrets", new Service(cache, grantService, specLifecycleService, specRegistry));
    }
}
