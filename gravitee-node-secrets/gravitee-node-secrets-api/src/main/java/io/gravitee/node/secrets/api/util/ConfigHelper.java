package io.gravitee.node.secrets.api.util;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.keyvalue.AbstractKeyValue;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConfigHelper {

    public static Map<String, Object> removePrefix(Map<String, Object> propertiesMap, String prefix) {
        return propertiesMap
            .entrySet()
            .stream()
            .filter(e -> e.getKey().startsWith(prefix))
            // chopping the prefix out of the key
            .map(e -> new DefaultMapEntry<>(e.getKey().substring(prefix.length() + 1), e.getValue()))
            .collect(Collectors.toMap(AbstractKeyValue::getKey, AbstractKeyValue::getValue));
    }
}
