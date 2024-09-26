package com.graviteesource.services.runtimesecrets.grant;

import static com.graviteesource.services.runtimesecrets.config.Config.ALLOW_EMPTY_ACL_SPECS;
import static com.graviteesource.services.runtimesecrets.config.Config.ALLOW_ON_THE_FLY_SPECS;
import static io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation.PLUGIN_KIND;

import com.graviteesource.services.runtimesecrets.config.Config;
import com.graviteesource.services.runtimesecrets.errors.SecretSpecNotFoundException;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation;
import io.gravitee.node.api.secrets.runtime.grant.GrantService;
import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import java.util.Arrays;
import java.util.Objects;
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
    public boolean authorize(@Nonnull DiscoveryContext context, Spec spec) {
        if (spec == null) {
            throw new SecretSpecNotFoundException(
                "no spec found or created on-the-fly for ref [%s] in envId [%s], %s=%s".formatted(
                        context.ref().rawRef(),
                        context.envId(),
                        ALLOW_ON_THE_FLY_SPECS,
                        config.allowOnTheFlySpecs()
                    )
            );
        }
        if (spec.acls() == null) {
            if (!config.allowEmptyACLSpecs()) {
                log.warn(
                    "secret spec for ref [{}] is not granted because is does not contains ACLs and this is not allowed. see: {}",
                    context.ref().rawRef(),
                    ALLOW_EMPTY_ACL_SPECS
                );
                return false;
            } else {
                return Objects.equals(context.envId(), spec.envId());
            }
        }

        return checkACLs(spec, context);
    }

    public boolean isGranted(@Nonnull String token) {
        return grantRegistry.exists(token);
    }

    @Override
    public void grant(@Nonnull DiscoveryContext context) {
        grantRegistry.register(context);
    }

    @Override
    public void revoke(@Nonnull DiscoveryContext context) {
        grantRegistry.unregister(context);
    }

    private boolean checkACLs(Spec spec, DiscoveryContext context) {
        Predicate<ACLs> noDefKind = acls ->
            acls.definitions() == null ||
            acls.definitions().isEmpty() ||
            acls.definitions().stream().allMatch(def -> def.kind() == null || def.kind().isEmpty());

        Predicate<ACLs> defKind = acls ->
            acls.definitions().stream().anyMatch(defACLs -> defACLs.kind().contains(context.location().definition().kind()));

        Predicate<ACLs> noDefId = acls ->
            acls.definitions() == null ||
            acls.definitions().isEmpty() ||
            acls.definitions().stream().allMatch(def -> def.ids() == null || def.ids().isEmpty());

        Predicate<ACLs> defId = acls ->
            acls.definitions().stream().anyMatch(defACLs -> defACLs.ids().contains(context.location().definition().id()));

        Predicate<ACLs> noPlugin = acls -> acls.plugins() == null || acls.plugins().isEmpty();

        Predicate<ACLs> plugin = acls ->
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

        return noDefKind.or(defKind).and(noDefId.or(defId)).and(noPlugin.or(plugin)).test(spec.acls());
    }
}
