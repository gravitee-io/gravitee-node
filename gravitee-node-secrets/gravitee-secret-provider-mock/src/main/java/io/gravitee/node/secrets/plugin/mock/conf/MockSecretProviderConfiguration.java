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

    private static final String DATA_PROP = ".data";
    private static final String SECRET_PROP = ".secret";
    private static final String LOOP_PROP = ".loop";
    private static final String TYPE_PROP = ".type";
    private static final String REPEAT_PROP = ".repeat";
    private static final String MESSAGE_PROP = ".message";
    private static final String ERROR_PROP = ".error";
    private final boolean enabled;
    private static final int NO_REPEAT = 0;

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
        while (config.containsKey(error + SECRET_PROP)) {
            String secret = config.get(error + SECRET_PROP).toString();
            int repeat = Integer.parseInt(config.getOrDefault(error + REPEAT_PROP, String.valueOf(NO_REPEAT)).toString());
            configuredErrors.put(secret, new ConfiguredError(config.getOrDefault(error + MESSAGE_PROP, "").toString(), repeat));
            error = error(++i);
        }

        // process watch
        i = 0;
        String event = event(i);
        while (watches.containsKey(event + SECRET_PROP)) {
            configuredEvents.add(
                new ConfiguredEvent(
                    watches.get(event + SECRET_PROP).toString(),
                    SecretEvent.Type.valueOf(watches.getOrDefault(event + TYPE_PROP, "CREATED").toString()),
                    ConfigHelper.removePrefix(watches, event + DATA_PROP),
                    String.valueOf(watches.get(event + ERROR_PROP))
                )
            );
            event = event(++i);
        }

        // process renewal
        i = 0;
        String renewal = renewal(i);
        while (config.containsKey(renewal + SECRET_PROP)) {
            String secret = config.get(renewal + SECRET_PROP).toString();
            boolean loop = ConfigHelper.getProperty(config, renewal + LOOP_PROP, Boolean.class, false);

            List<Map<String, Object>> revisions = new ArrayList<>();
            int r = 0;
            Map<String, Object> data = ConfigHelper.removePrefix(config, revision(renewal, r) + DATA_PROP);
            while (!data.isEmpty()) {
                revisions.add(data);
                data = ConfigHelper.removePrefix(config, revision(renewal, ++r) + DATA_PROP);
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
