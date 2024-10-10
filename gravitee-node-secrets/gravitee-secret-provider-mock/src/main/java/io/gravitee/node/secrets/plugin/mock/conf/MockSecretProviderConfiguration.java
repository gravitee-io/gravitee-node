package io.gravitee.node.secrets.plugin.mock.conf;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.model.SecretEvent;
import io.gravitee.node.api.secrets.util.ConfigHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@FieldNameConstants
public class MockSecretProviderConfiguration implements SecretManagerConfiguration {

    private final boolean enabled;
    public static final int NO_REPEAT = 0;

    @Getter
    private final Map<String, Object> secrets;

    private final Map<String, ConfiguredError> configuredErrors = new ConcurrentHashMap<>();
    private final Map<String, Renewal> configuredRenewals = new ConcurrentHashMap<>();
    private final List<ConfiguredEvent> configuredEvents = new ArrayList<>();
    private final Long watchesDelayDuration;
    private final TimeUnit watchesDelayUnit;

    public MockSecretProviderConfiguration(Map<String, Object> config) {
        this.enabled = ConfigHelper.getProperty(config, Fields.enabled, Boolean.class, false);
        this.secrets = ConfigHelper.removePrefix(config, "secrets");
        Map<String, Object> watches = ConfigHelper.removePrefix(config, "watches");
        watchesDelayUnit = TimeUnit.valueOf(watches.getOrDefault("delay.unit", "SECONDS").toString());
        watchesDelayDuration = Long.parseLong(watches.getOrDefault("delay.duration", "1").toString());

        // process errors
        int i = 0;
        String error = error(i);
        while (config.containsKey(error + ".secret")) {
            String secret = config.get(error + ".secret").toString();
            int repeat = Integer.parseInt(config.getOrDefault(error + ".repeat", String.valueOf(NO_REPEAT)).toString());
            configuredErrors.put(secret, new ConfiguredError(config.getOrDefault(error + ".message", "").toString(), repeat));
            error = error(++i);
        }

        // process watch
        i = 0;
        String event = event(i);
        while (watches.containsKey(event + ".secret")) {
            configuredEvents.add(
                new ConfiguredEvent(
                    watches.get(event + ".secret").toString(),
                    SecretEvent.Type.valueOf(watches.getOrDefault(event + ".type", "CREATED").toString()),
                    ConfigHelper.removePrefix(watches, event + ".data"),
                    String.valueOf(watches.get(event + ".error"))
                )
            );
            event = event(++i);
        }

        // process renewal
        i = 0;
        String renewal = renewal(i);
        while (config.containsKey(renewal + ".secret")) {
            String secret = config.get(renewal + ".secret").toString();
            boolean loop = ConfigHelper.getProperty(config, renewal + ".loop", Boolean.class, false);

            List<Map<String, Object>> revisions = new ArrayList<>();
            int r = 0;
            Map<String, Object> data = ConfigHelper.removePrefix(config, revision(renewal, r) + ".data");
            while (!data.isEmpty()) {
                revisions.add(data);
                data = ConfigHelper.removePrefix(config, revision(renewal, ++r) + ".data");
            }
            configuredRenewals.put(secret, new Renewal(loop, revisions));
            renewal = renewal(++i);
        }
    }

    private static String event(int i) {
        return "events[%s]".formatted(i);
    }

    private static String error(int i) {
        return "errors[%s]".formatted(i);
    }

    private static String renewal(int i) {
        return "renewals[%s]".formatted(i);
    }

    private static String revision(String base, int j) {
        return "%s.revisions[%s]".formatted(base, j);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Optional<ConfiguredError> getError(String secret) {
        return Optional.ofNullable(configuredErrors.get(secret));
    }
}
