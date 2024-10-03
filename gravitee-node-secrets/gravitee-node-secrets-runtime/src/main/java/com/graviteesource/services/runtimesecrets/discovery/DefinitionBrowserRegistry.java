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
package com.graviteesource.services.runtimesecrets.discovery;

import io.gravitee.node.api.secrets.runtime.discovery.DefinitionBrowser;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefinitionBrowserRegistry {

    private final Collection<DefinitionBrowser> browsers;

    @Autowired
    public DefinitionBrowserRegistry(Collection<DefinitionBrowser> browsers) {
        this.browsers = browsers;
    }

    public <T> Optional<DefinitionBrowser<T>> findBrowser(T definition) {
        for (DefinitionBrowser<T> browser : this.browsers) {
            if (browser.canHandle(definition)) {
                return Optional.of(browser);
            }
        }
        return Optional.empty();
    }
}
