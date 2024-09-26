package io.gravitee.node.api.secrets.runtime.discovery;

import java.util.Optional;

public record Definition(String kind, String id, Optional<String> revision) {}
