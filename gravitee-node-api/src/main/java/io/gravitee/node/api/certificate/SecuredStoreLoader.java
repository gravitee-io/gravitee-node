package io.gravitee.node.api.certificate;

import java.util.function.Consumer;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface SecuredStoreLoader<K> {
    String CERTIFICATE_FORMAT_JKS = "JKS";
    String CERTIFICATE_FORMAT_PEM = "PEM";
    String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";

    void start();

    void stop();

    void addListener(Consumer<K> listener);
}
