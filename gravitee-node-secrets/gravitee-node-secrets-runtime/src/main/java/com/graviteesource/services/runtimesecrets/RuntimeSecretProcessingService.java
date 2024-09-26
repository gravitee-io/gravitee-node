package com.graviteesource.services.runtimesecrets;

import com.graviteesource.services.runtimesecrets.discovery.ContextRegistry;
import com.graviteesource.services.runtimesecrets.discovery.DefinitionBrowserRegistry;
import com.graviteesource.services.runtimesecrets.discovery.PayloadRefParser;
import com.graviteesource.services.runtimesecrets.discovery.RefParser;
import com.graviteesource.services.runtimesecrets.el.Formatter;
import com.graviteesource.services.runtimesecrets.spec.registry.EnvAwareSpecRegistry;
import io.gravitee.node.api.secrets.runtime.discovery.*;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import java.util.*;
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
public class RuntimeSecretProcessingService {

    private final DefinitionBrowserRegistry definitionBrowserRegistry;
    private final ContextRegistry contextRegistry;
    private final GrantService grantService;
    private final SpecLifecycleService specLifecycleService;
    private final EnvAwareSpecRegistry specRegistry;

    /**
     * <li>finds a {@link DefinitionBrowser}</li>
     * <li>Run it to get {@link DiscoveryContext}</li>
     * <li>Inject EL {@link PayloadRefParser}</li>
     * <li>Find {@link Spec}</li>
     * <li>Grant {@link DiscoveryContext}</li>
     * @param definition the secret naturalId container
     * @param metadata some optional metadata
     * @param <T> the kind of subject
     */
    <T> void processSecrets(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        Optional<DefinitionBrowser<T>> browser = definitionBrowserRegistry.findBrowser(definition);
        if (browser.isEmpty()) {
            log.info("No definition browser found for kind [{}]", definition.getClass());
            return;
        }

        DefinitionBrowser<T> definitionBrowser = browser.get();
        Definition rootDefinition = definitionBrowser.getDefinitionKindLocation(definition, metadata);
        DefaultPayloadNotifier notifier = new DefaultPayloadNotifier(rootDefinition, envId);
        definitionBrowser.findPayloads(notifier);

        // register contexts by naturalId and definition
        for (DiscoveryContext context : notifier.getContextList()) {
            contextRegistry.register(context, rootDefinition);
            // get spec
            Spec spec = specRegistry.fromRef(context.envId(), context.ref());
            if (spec == null && specLifecycleService.shouldDeployOnTheFly(context.ref())) {
                spec = specLifecycleService.deployOnTheFly(envId, context.ref());
            }
            boolean granted = grantService.authorize(context, spec);
            if (granted && context.ref().mainExpression().isLiteral()) {
                grantService.grant(context);
            }
        }
    }

    static class DefaultPayloadNotifier implements DefinitionPayloadNotifier {

        @Getter
        final List<DiscoveryContext> contextList = new ArrayList<>();

        final Definition rootDefinition;
        final String envId;

        DefaultPayloadNotifier(Definition rootDefinition, String envId) {
            this.rootDefinition = rootDefinition;
            this.envId = envId;
        }

        @Override
        public void onPayload(String payload, PayloadLocation payloadLocation) {
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
        }
    }
}
