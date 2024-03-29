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
package io.gravitee.node.container.spring.env;

import io.gravitee.common.util.RelaxedPropertySource;
import java.util.Map;
import org.springframework.context.ApplicationContext;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeEnvironmentPropertySource extends AbstractGraviteePropertySource {

    private final RelaxedPropertySource relaxedPropertySource;

    public GraviteeEnvironmentPropertySource(String name, Map<String, Object> source, ApplicationContext applicationContext) {
        super(name, source, applicationContext);
        this.relaxedPropertySource = new RelaxedPropertySource("envVariables", source);
    }

    @Override
    protected Object getValue(String key) {
        return relaxedPropertySource.getProperty(key);
    }

    @Override
    public boolean containsProperty(String name) {
        return relaxedPropertySource.containsProperty(name);
    }

    @Override
    public String[] getPropertyNames() {
        return relaxedPropertySource.getPropertyNames();
    }
}
