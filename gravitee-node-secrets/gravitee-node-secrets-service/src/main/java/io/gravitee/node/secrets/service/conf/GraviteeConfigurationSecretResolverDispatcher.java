package io.gravitee.node.secrets.service.conf;

import io.gravitee.node.secrets.SecretProviderPluginManager;
import io.gravitee.node.secrets.api.SecretManagerConfiguration;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.service.AbstractSecretProviderDispatcher;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.keyvalue.AbstractKeyValue;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.springframework.core.env.*;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeConfigurationSecretResolverDispatcher extends AbstractSecretProviderDispatcher {

    private static final String SECRETS_CONFIG_KEY = "secrets";
    private final Environment environment;

    @Getter
    @Accessors(fluent = true)
    private List<String> enabledManagers = new ArrayList<>();

    public GraviteeConfigurationSecretResolverDispatcher(SecretProviderPluginManager secretProviderPluginManager, Environment environment)
        throws Exception {
        super(secretProviderPluginManager);
        this.environment = environment;
        init();
    }

    @Override
    public List<String> filterEnabledManagers(Collection<String> secretProvidersPluginIds) {
        record SecretManagerEnablement(String id, int order) implements Comparable<SecretManagerEnablement> {
            @Override
            public int compareTo(SecretManagerEnablement o) {
                return Integer.compare(this.order, o.order);
            }
        }

        Set<SecretManagerEnablement> orderedManagers = new TreeSet<>();

        for (String id : secretProvidersPluginIds) {
            boolean enabled = environment.getProperty(String.format("%s.%s.enabled", SECRETS_CONFIG_KEY, id), boolean.class, false);
            int order = environment.getProperty(String.format("%s.%s.order", SECRETS_CONFIG_KEY, id), int.class, 0);
            if (enabled) {
                orderedManagers.add(new SecretManagerEnablement(id, order));
            }
        }

        this.enabledManagers = orderedManagers.stream().map(SecretManagerEnablement::id).toList();
        return enabledManagers();
    }

    @Override
    public <T extends SecretManagerConfiguration> T readConfiguration(String managerId, Class<?> configurationClass) {
        Map<String, Object> configurationProperties = getChoppedPropertiesStartingWith(
            (ConfigurableEnvironment) environment,
            String.format("%s.%s", SECRETS_CONFIG_KEY, managerId)
        );

        try {
            @SuppressWarnings("unchecked")
            Constructor<T> constructor = (Constructor<T>) configurationClass.getDeclaredConstructor(Map.class);
            return constructor.newInstance(configurationProperties);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new SecretManagerConfigurationException("Could not create configuration class for secret manager: " + managerId, e);
        }
    }

    private Map<String, Object> getChoppedPropertiesStartingWith(ConfigurableEnvironment env, String prefix) {
        Map<String, Object> result = new HashMap<>();
        env.getPropertySources().forEach(ps -> result.putAll(getAllProperties(ps)));
        return result
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            // chopping the prefix out of the key
            .map(e -> new DefaultMapEntry<>(e.getKey().substring(prefix.length() + 1), e.getValue()))
            .collect(Collectors.toMap(AbstractKeyValue::getKey, AbstractKeyValue::getValue));
    }

    private Map<String, Object> getAllProperties(PropertySource<?> propertySource) {
        Map<String, Object> result = new HashMap<>();

        if (propertySource instanceof CompositePropertySource cps) {
            cps.getPropertySources().forEach(ps -> result.putAll(getAllProperties(ps)));
            return result;
        }

        if (propertySource instanceof EnumerablePropertySource<?> ps) {
            Arrays.asList(ps.getPropertyNames()).forEach(key -> result.put(key, ps.getProperty(key)));
            return result;
        }

        return result;
    }
}
