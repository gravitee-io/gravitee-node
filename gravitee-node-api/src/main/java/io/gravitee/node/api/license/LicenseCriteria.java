package io.gravitee.node.api.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * License search criteria.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LicenseCriteria {

    /**
     * The type of license search (e.g. ORGANIZATION).
     */
    private String referenceType;

    /**
     * The minimum date for the license update date. -1 means no limit.
     */
    @Builder.Default
    private final long from = -1;

    /**
     * The maximum date for the license update date. -1 means no limit..
     */
    @Builder.Default
    private final long to = -1;
}
