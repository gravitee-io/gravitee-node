package io.gravitee.node.api.certificate;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CRLLoaderOptions {

    private final String path;

    public boolean isConfigured() {
        return path != null && !path.isEmpty();
    }
}
