package io.gravitee.node.api.certificate;

import java.security.cert.CRL;
import java.util.List;
import java.util.function.Consumer;

public interface CRLLoader extends IdProvider {
    void start();

    void stop();

    void setEventHandler(Consumer<List<CRL>> handler);
}
