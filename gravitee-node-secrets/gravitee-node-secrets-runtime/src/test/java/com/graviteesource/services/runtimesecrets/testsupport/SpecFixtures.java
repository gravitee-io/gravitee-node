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
package com.graviteesource.services.runtimesecrets.testsupport;

import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.List;
import java.util.UUID;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SpecFixtures {

    public static Spec namedWithDynKey(String envId, String name, String uri) {
        return new Spec(UUID.randomUUID().toString(), name, uri, null, null, true, false, null, null, envId);
    }

    public static Spec fromUriDynKey(String envId, String uri) {
        return new Spec(UUID.randomUUID().toString(), null, uri, null, null, true, false, null, null, envId);
    }

    public static Spec fromUriAndKey(String envId, String uri, String key) {
        return new Spec(UUID.randomUUID().toString(), null, uri, key, null, false, false, null, null, envId);
    }

    public static Spec namedWithUriAndKey(String envId, String name, String uri, String key) {
        return new Spec(UUID.randomUUID().toString(), name, uri, key, null, false, false, null, null, envId);
    }

    public static Spec fromNameUriAndKeyACLs(
        String envId,
        String name,
        String uri,
        String key,
        ACLs.DefinitionACL definitionACL,
        ACLs.PluginACL pluginACL
    ) {
        return new Spec(
            UUID.randomUUID().toString(),
            name,
            uri,
            key,
            null,
            false,
            false,
            null,
            new ACLs(List.of(definitionACL), List.of(pluginACL)),
            envId
        );
    }

    public static Spec fromNameUriAndKeyPluginACL(String envId, String name, String uri, String key, ACLs.PluginACL pluginACL) {
        return new Spec(UUID.randomUUID().toString(), name, uri, key, null, false, false, null, new ACLs(null, List.of(pluginACL)), envId);
    }
}
