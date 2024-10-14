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
package com.graviteesource.services.runtimesecrets.grant;

import static com.graviteesource.services.runtimesecrets.config.Config.DENY_SPEC_WITHOUT_ACLS;
import static com.graviteesource.services.runtimesecrets.config.Config.ON_THE_FLY_SPECS_ENABLED;
import static io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation.PLUGIN_KIND;

import com.graviteesource.services.runtimesecrets.config.Config;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import io.gravitee.node.api.secrets.runtime.grant.Grant;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultGrantService implements GrantService {

    private final GrantRegistry grantRegistry;
    private final Config config;

    @Override
    public boolean grant(@Nonnull DiscoveryContext context, Spec spec) {
        boolean granted = isGranted(context, spec);
        if (granted && context.id() != null) {
            grantRegistry.register(context.id().toString(), new Grant(CacheKey.from(spec), spec.key()));
        }
        return granted;
    }

    private boolean isGranted(DiscoveryContext context, Spec spec) {
        if (spec == null) {
            log.warn(
                "no spec found for ref {} in envId [{}], {}={}",
                context.ref().rawRef(),
                context.envId(),
                ON_THE_FLY_SPECS_ENABLED,
                config.onTheFlySpecs().enabled()
            );
            return false;
        }
        if (spec.acls() == null) {
            if (config.denySpecWithoutACLs()) {
                log.warn(
                    "secret spec for ref [{}] not granted because secrets requires ACLs. see conf: {}",
                    context.ref().rawRef(),
                    DENY_SPEC_WITHOUT_ACLS
                );
                return false;
            } else {
                return checkSpec(context).test(spec);
            }
        }

        return checkSpec(context).test(spec) && checkACLs(context).test(spec.acls());
    }

    @Override
    public Optional<Grant> getGrant(String contextId) {
        return Optional.ofNullable(grantRegistry.get(contextId));
    }

    @Override
    public void revoke(@Nonnull DiscoveryContext context) {
        grantRegistry.unregister(context);
    }

    private Predicate<Spec> checkSpec(DiscoveryContext context) {
        Predicate<Spec> envMatch = spec -> Objects.equals(context.envId(), spec.envId());

        Predicate<Spec> dynOrNoKey = spec -> spec.usesDynamicKey() || context.ref().secondaryType() == null;

        Predicate<Spec> keyMatch = spec ->
            context.ref().secondaryType() == Ref.SecondaryType.KEY &&
            context.ref().secondaryExpression().isLiteral() &&
            spec.key().equals(context.ref().secondaryExpression().value());

        return envMatch.and(keyMatch.or(dynOrNoKey));
    }

    private Predicate<ACLs> checkACLs(DiscoveryContext context) {
        Predicate<ACLs> noDefKind = acls ->
            acls.definitions() == null ||
            acls.definitions().isEmpty() ||
            acls.definitions().stream().allMatch(def -> def.kind() == null || def.kind().isEmpty());

        Predicate<ACLs> defKindMatch = acls ->
            acls.definitions().stream().anyMatch(defACLs -> defACLs.kind().contains(context.location().definition().kind()));

        Predicate<ACLs> noDefId = acls ->
            acls.definitions() == null ||
            acls.definitions().isEmpty() ||
            acls.definitions().stream().allMatch(def -> def.ids() == null || def.ids().isEmpty());

        Predicate<ACLs> defIdMatch = acls ->
            acls.definitions().stream().anyMatch(defACLs -> defACLs.ids().contains(context.location().definition().id()));

        Predicate<ACLs> noPlugin = acls -> acls.plugins() == null || acls.plugins().isEmpty();

        Predicate<ACLs> pluginMatch = acls ->
            acls
                .plugins()
                .stream()
                .anyMatch(pluginACL ->
                    Objects.equals(
                        pluginACL.id(),
                        Arrays
                            .stream(context.location().payloadLocations())
                            .filter(pl -> pl.kind().equals(PLUGIN_KIND))
                            .findFirst()
                            .orElse(PayloadLocation.NOWHERE)
                            .id()
                    )
                );

        return noDefKind.or(defKindMatch).and(noDefId.or(defIdMatch)).and(noPlugin.or(pluginMatch));
    }
}
