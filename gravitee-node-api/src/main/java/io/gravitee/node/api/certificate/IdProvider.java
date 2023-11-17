package io.gravitee.node.api.certificate;

/**
 * This present a simple abstraction that tells its impletation that it should provider identifier
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team *
 *
 */
public interface IdProvider {
    /**
     *
     * @return an id as a String
     */
    String id();
}
