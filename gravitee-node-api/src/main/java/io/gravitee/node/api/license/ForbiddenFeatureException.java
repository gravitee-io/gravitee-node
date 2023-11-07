package io.gravitee.node.api.license;

import java.io.Serial;
import java.util.Collection;
import lombok.Getter;

/**
 * Exception thrown when a feature is not allowed to be used.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ForbiddenFeatureException extends Exception {

    @Serial
    private static final long serialVersionUID = -1786513994776272636L;

    @Getter
    private final Collection<LicenseManager.ForbiddenFeature> features;

    public ForbiddenFeatureException(Collection<LicenseManager.ForbiddenFeature> features) {
        this.features = features;
    }

    @Override
    public String getMessage() {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (LicenseManager.ForbiddenFeature forbiddenFeature : features) {
            if (!first) {
                builder.append("\n");
            }
            builder.append(
                String.format(
                    "Plugin [%s] cannot be loaded because the feature [%s] is not allowed by the license.",
                    forbiddenFeature.plugin(),
                    forbiddenFeature.feature()
                )
            );
        }

        return builder.toString();
    }
}
