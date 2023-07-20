package io.gravitee.node.secrets.api;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecretManagerConfiguration extends Comparable<SecretManagerConfiguration> {
    int getPriority();

    boolean isEnabled();

    @Override
    default int compareTo(SecretManagerConfiguration o) {
        return Integer.compare(getPriority(), o.getPriority());
    }
}
