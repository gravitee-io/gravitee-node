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

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.gravitee.node.api.secrets.runtime.discovery.ContextRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.Definition;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultContextRegistry implements ContextRegistry {

    Map<String, ContextRegistry> registry = new HashMap<>();

    public void register(DiscoveryContext context, Definition definition) {
        registry(context.envId()).register(context, definition);
    }

    public List<DiscoveryContext> findBySpec(Spec spec) {
        return registry(spec.envId()).findBySpec(spec);
    }

    public List<DiscoveryContext> getByDefinition(String envId, Definition definition) {
        return registry(envId).getByDefinition(null, definition);
    }

    ContextRegistry registry(String envId) {
        return registry.computeIfAbsent(envId, ignore -> new InternalRegistry());
    }

    static class InternalRegistry implements ContextRegistry {

        private final Multimap<String, DiscoveryContext> byName = MultimapBuilder.hashKeys().arrayListValues().build();
        private final Multimap<String, DiscoveryContext> byUri = MultimapBuilder.hashKeys().arrayListValues().build();
        private final Multimap<String, DiscoveryContext> byUriAndKey = MultimapBuilder.hashKeys().arrayListValues().build();
        private final Multimap<Definition, DiscoveryContext> byDefinitionSpec = MultimapBuilder.hashKeys().arrayListValues().build();

        public void register(DiscoveryContext context, Definition definition) {
            if (context.ref().mainType() == Ref.MainType.NAME && context.ref().mainExpression().isLiteral()) {
                byName.put(context.ref().mainExpression().value(), context);
            }
            if (context.ref().mainType() == Ref.MainType.URI && context.ref().mainExpression().isLiteral()) {
                byUri.put(context.ref().mainExpression().value(), context);
                if (context.ref().secondaryType() == Ref.SecondaryType.KEY && context.ref().secondaryExpression().isLiteral()) {
                    byUriAndKey.put(context.ref().uriAndKey(), context);
                }
            }
            byDefinitionSpec.put(definition, context);
        }

        public List<DiscoveryContext> findBySpec(Spec spec) {
            List<DiscoveryContext> result = new ArrayList<>();
            if (spec.name() != null && !spec.name().isEmpty()) {
                result.addAll(byName.get(spec.name()));
            }
            if (spec.uri() != null && !spec.uri().isEmpty()) {
                result.addAll(byUri.get(spec.name()));
            }
            if (spec.key() != null && !spec.key().isEmpty()) {
                result.addAll(byUriAndKey.get(spec.uriAndKey()));
            }
            return result;
        }

        public List<DiscoveryContext> getByDefinition(String envId, Definition definition) {
            return (List<DiscoveryContext>) byDefinitionSpec.get(definition);
        }
    }
}
