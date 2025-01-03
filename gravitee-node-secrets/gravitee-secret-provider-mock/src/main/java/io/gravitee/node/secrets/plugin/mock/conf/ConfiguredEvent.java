package io.gravitee.node.secrets.plugin.mock.conf;

import io.gravitee.secrets.api.core.SecretEvent;
import java.util.Map;

public record ConfiguredEvent(String secret, SecretEvent.Type type, Map<String, Object> data, String error) {}
