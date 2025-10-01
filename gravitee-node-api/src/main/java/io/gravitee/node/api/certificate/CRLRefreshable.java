package io.gravitee.node.api.certificate;

import java.security.cert.CRL;
import java.util.List;

public interface CRLRefreshable {
    void refresh(List<CRL> crls);
}
