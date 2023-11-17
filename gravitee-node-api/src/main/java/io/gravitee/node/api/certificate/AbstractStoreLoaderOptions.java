package io.gravitee.node.api.certificate;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@SuperBuilder
public abstract class AbstractStoreLoaderOptions {

    private final boolean watch;
}
