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

    /**
     * Whether these options point at anything to load from. The default options every server is given, secured
     * or not, carry a type and no source at all, which is how an operator who configured a store is told apart
     * from one who did not.
     * <p>
     * Only the fields every store shares are tested here. A subclass with sources of its own overrides this.
     *
     * @return {@code true} when a type and at least one source (paths, secret or Kubernetes location) are set.
     */
    public boolean namesASource() {
        return (
            type != null &&
            ((paths != null && !paths.isEmpty()) ||
                secretLocation != null ||
                (kubernetesLocations != null && !kubernetesLocations.isEmpty()))
        );
    }
}
