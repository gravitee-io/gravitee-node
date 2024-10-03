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
        String base = error(i);
        while (config.containsKey(base + ".secret")) {
            String secret = config.get(base + ".secret").toString();
            int repeat = Integer.parseInt(config.getOrDefault(base + ".repeat", String.valueOf(NO_REPEAT)).toString());
            configuredErrors.put(secret, new ConfiguredError(config.getOrDefault(base + ".message", "").toString(), repeat));
            base = error(++i);
        }

        // process watch
        i = 0;
        base = event(i);
        while (watches.containsKey(base + ".secret")) {
            configuredEvents.add(
                new ConfiguredEvent(
                    watches.get(base + ".secret").toString(),
                    SecretEvent.Type.valueOf(watches.getOrDefault(base + ".type", "CREATED").toString()),
                    ConfigHelper.removePrefix(watches, base + ".data"),
                    String.valueOf(watches.get(base + ".error"))
                )
            );
            base = event(++i);
        }
    }

    private static String event(int i) {
        return "events[%s]".formatted(i);
    }

    private static String error(int i) {
        return "errors[%s]".formatted(i);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Optional<ConfiguredError> getError(String secret) {
        return Optional.ofNullable(configuredErrors.get(secret));
    }
}
