package io.gravitee.node.secrets.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
public class SecretAccessContext {

    private final SecretScope scope;
    private final String owner;
}
