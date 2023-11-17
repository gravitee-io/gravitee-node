package io.gravitee.node.api.certificate;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 *
 *  Common properties when loading trustore and keystore.
 *  @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 *
 */
@Getter
@SuperBuilder
public abstract class AbstractStoreLoaderOptions {

    private static final boolean DEFAULT_WATCH = true;
    private static final String DEFAULT_PASSWORD = UUID.randomUUID().toString();

    private final List<String> paths;
    private final String type;
    private final String secretLocation;
    private final List<String> kubernetesLocations;

    @Builder.Default
    private String password = DEFAULT_PASSWORD;

    @Builder.Default
    private boolean watch = DEFAULT_WATCH;
}
