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
package com.graviteesource.services.runtimesecrets;

import com.graviteesource.services.runtimesecrets.discovery.DefinitionBrowserRegistry;
import com.graviteesource.services.runtimesecrets.discovery.PayloadRefParser;
import com.graviteesource.services.runtimesecrets.discovery.RefParser;
import com.graviteesource.services.runtimesecrets.el.Formatter;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.*;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import java.util.*;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@RequiredArgsConstructor
public class Processor {

    private final DefinitionBrowserRegistry definitionBrowserRegistry;
    private final ContextRegistry contextRegistry;
    private final SpecRegistry specRegistry;
    private final GrantService grantService;
    private final SpecLifecycleService specLifecycleService;

    /**
     * <li>finds a {@link DefinitionBrowser}</li>
     * <li>Run it to get {@link DiscoveryContext}</li>
     * <li>Inject EL {@link PayloadRefParser}</li>
     * <li>Find {@link Spec} or create on the fly</li>
     * <li>Grant {@link DiscoveryContext}</li>
     * @param definition the secret ref container
     * @param metadata some optional metadata
     * @param <T> the kind of subject
     */
    public <T> void onDefinitionDeploy(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        Optional<DefinitionBrowser<T>> browser = getDefinitionBrowser(definition);
        if (browser.isEmpty()) {
            return;
        }

        DefinitionBrowser<T> definitionBrowser = browser.get();
        Definition rootDefinition = definitionBrowser.getDefinitionLocation(definition, metadata);

        log.info("Finding secret in definition: {}", rootDefinition);

        DefaultPayloadNotifier notifier = new DefaultPayloadNotifier(rootDefinition, envId, specRegistry);
        definitionBrowser.findPayloads(definition, notifier);

        // register contexts by ref and definition
        for (DiscoveryContext context : notifier.getContextList()) {
            contextRegistry.register(context, rootDefinition);
            // get spec
            Spec spec = specRegistry.fromRef(context.envId(), context.ref());
            if (spec == null && specLifecycleService.shouldDeployOnTheFly(context.ref())) {
                spec = specLifecycleService.deployOnTheFly(envId, context.ref());
            }

            if (context.ref().mainExpression().isLiteral()) {
                boolean granted = grantService.grant(context, spec);
                if (granted) {
                    grantService.grant(context, spec);
                }
            }
        }
    }

    private <T> Optional<DefinitionBrowser<T>> getDefinitionBrowser(T definition) {
        Optional<DefinitionBrowser<T>> browser = definitionBrowserRegistry.findBrowser(definition);
        if (browser.isEmpty()) {
            log.info("No definition browser found for kind [{}]", definition.getClass());
        }
        return browser;
    }

    public <T> void onDefinitionUnDeploy(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        Optional<DefinitionBrowser<T>> browser = getDefinitionBrowser(definition);
        if (browser.isEmpty()) {
            return;
        }

        Definition rootDefinition = browser.get().getDefinitionLocation(definition, metadata);
        List<DiscoveryContext> currentDefinitionContexts = contextRegistry.getByDefinition(envId, rootDefinition);

        // undeploy unused on-the-fly spec
        record ContextBySpec(Spec spec, long count) {}
        currentDefinitionContexts
            .stream()
            .map(context -> specRegistry.fromRef(envId, context.ref()))
            .filter(Spec::isOnTheFly)
            .map(spec -> {
                // count context using this spec, excluding the current definition's
                long count = contextRegistry
                    .findBySpec(spec)
                    .stream()
                    .filter(context1 -> !currentDefinitionContexts.contains(context1))
                    .count();
                return new ContextBySpec(spec, count);
            })
            // when this spec will not be used after definition is undeploy
            .filter(contextBySpec -> contextBySpec.count == 0L)
            .map(ContextBySpec::spec)
            .forEach(specLifecycleService::undeploy);

        // revoke and remove all contexts
        currentDefinitionContexts.forEach(context -> {
            grantService.revoke(context);
            contextRegistry.unregister(context, rootDefinition);
        });
    }

    static final class DefaultPayloadNotifier implements DefinitionPayloadNotifier {

        @Getter
        final List<DiscoveryContext> contextList = new ArrayList<>();

        final Definition rootDefinition;
        final String envId;
        final SpecRegistry specRegistry;

        DefaultPayloadNotifier(Definition rootDefinition, String envId, SpecRegistry specRegistry) {
            this.rootDefinition = rootDefinition;
            this.envId = envId;
            this.specRegistry = specRegistry;
        }

        @Override
        public void onPayload(String payload, PayloadLocation payloadLocation, Consumer<String> updatedPayload) {
            // no op on empty payloads
            if (payload == null || payload.isBlank()) {
                updatedPayload.accept(payload);
                return;
            }
            PayloadRefParser payloadRefParser = new PayloadRefParser(payload);
            List<DiscoveryContext> discoveryContexts = payloadRefParser
                .runDiscovery()
                .stream()
                .map(raw -> RefParser.parse(raw.ref()))
                .map(ref ->
                    new DiscoveryContext(
                        UUID.randomUUID(),
                        envId,
                        ref,
                        new DiscoveryLocation(
                            new DiscoveryLocation.Definition(this.rootDefinition.kind(), this.rootDefinition.id()),
                            payloadLocation
                        )
                    )
                )
                .toList();
            contextList.addAll(discoveryContexts);
            List<String> ELs = discoveryContexts
                .stream()
                .map(context -> {
                    if (context.ref().mainExpression().isLiteral()) {
                        return Formatter.computeELFromStatic(context, envId);
                    } else {
                        return Formatter.computeELFromEL(context, envId);
                    }
                })
                .toList();
            payloadRefParser.replaceRefs(ELs);
            updatedPayload.accept(payloadRefParser.getUpdatePayload());
        }
    }
}
