/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.certificates.x509;

import io.gravitee.node.api.certificate.CRLRefreshable;
import io.gravitee.node.api.certificate.RefreshableX509Manager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class RefreshableX509TrustManagerDelegator extends X509ExtendedTrustManager implements RefreshableX509Manager, CRLRefreshable {

    /**
     * Send no certificate authority at all. An empty list means "no constraint" to a TLS client, which keeps
     * presenting its certificate while nothing is disclosed about what the listener accepts.
     */
    public static final Predicate<String> SEND_NOTHING = alias -> false;

    /**
     * Send every trusted certificate. Only appropriate when the whole trust store is operator-configured.
     */
    public static final Predicate<String> SEND_EVERYTHING = alias -> true;

    private final String target;
    private volatile List<CRL> crls = List.of();

    /**
     * The trust manager and the certificates sent as {@code certificate_authorities} always come from the same
     * refresh, so a handshake can never combine one with the other's. The sent certificates are deliberately
     * <b>not</b> the full set of trust anchors: entries registered dynamically at runtime (typically
     * per-subscription client certificates) must be trusted for validation, but must never reach the wire.
     */
    private record TrustMaterial(X509ExtendedTrustManager trustManager, List<X509Certificate> acceptedIssuers) {}

    private static final TrustMaterial EMPTY = new TrustMaterial(null, List.of());

    private volatile TrustMaterial trustMaterial = EMPTY;

    public RefreshableX509TrustManagerDelegator(String target) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
    }

    @Override
    public void refresh(KeyStore keyStore, char[] empty) {
        refresh(keyStore);
    }

    public void refresh(KeyStore keyStore) {
        refresh(keyStore, SEND_EVERYTHING);
    }

    /**
     * (Re)load the trust material.
     *
     * @param keyStore the complete trust store, used as-is for certificate validation.
     * @param advertisableAlias tells, for a given alias of {@code keyStore}, whether the matching certificate may be
     *                          advertised to clients through {@link #getAcceptedIssuers()}. Aliases that do not match
     *                          are still fully trusted, they are just kept out of the TLS handshake.
     */
    public void refresh(KeyStore keyStore, Predicate<String> advertisableAlias) {
        Objects.requireNonNull(keyStore, "cannot install null KeyStore");
        Objects.requireNonNull(advertisableAlias, "cannot install null alias predicate");
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            X509ExtendedTrustManager newTrustManager = (X509ExtendedTrustManager) trustManagerFactory.getTrustManagers()[0];
            List<X509Certificate> newAcceptedIssuers = collectAdvertisableIssuers(keyStore, advertisableAlias);
            // one volatile write, so a handshake never combines a new trust manager with a stale issuer list
            this.trustMaterial = new TrustMaterial(newTrustManager, newAcceptedIssuers);

            log.info(
                "Trust store has been (re)loaded with {} entries ({} sent as client certificate authorities) for target: {}",
                keyStore.size(),
                newAcceptedIssuers.size(),
                target
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create trust manager for target: %s".formatted(target), e);
        }
    }

    /**
     * Mirrors {@code sun.security.validator.TrustStoreUtil#getTrustedCerts(KeyStore)}, which is what the JDK trust
     * manager exposes as accepted issuers, restricted to the aliases the caller allows us to advertise. Key entries
     * count as trust anchors through the first certificate of their chain, and duplicates are collapsed, exactly as
     * the JDK does.
     */
    private static List<X509Certificate> collectAdvertisableIssuers(KeyStore keyStore, Predicate<String> advertisableAlias)
        throws KeyStoreException {
        if (advertisableAlias == SEND_NOTHING) {
            // the common case: skip walking the whole store only to produce an empty array
            return List.of();
        }
        Set<X509Certificate> issuers = new LinkedHashSet<>();
        for (String alias : Collections.list(keyStore.aliases())) {
            if (!advertisableAlias.test(alias)) {
                continue;
            }
            if (keyStore.isCertificateEntry(alias)) {
                addIfX509(issuers, keyStore.getCertificate(alias));
            } else if (keyStore.isKeyEntry(alias)) {
                Certificate[] chain = keyStore.getCertificateChain(alias);
                if (chain != null && chain.length > 0) {
                    addIfX509(issuers, chain[0]);
                }
            }
        }
        return List.copyOf(issuers);
    }

    private static void addIfX509(Set<X509Certificate> issuers, Certificate certificate) {
        if (certificate instanceof X509Certificate x509Certificate) {
            issuers.add(x509Certificate);
        }
    }

    @Override
    public void refresh(List<CRL> crls) {
        if (crls != null) {
            this.crls = List.copyOf(crls);
            log.info("CRL has been (re)loaded with {} entries for target: {}", crls.size(), target);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType);
        }
    }

    private void checkRevoked(X509Certificate[] x509Certificates) throws CertificateException {
        for (X509Certificate cert : x509Certificates) {
            for (CRL crl : crls) {
                if (crl.isRevoked(cert)) {
                    throw new CertificateException(
                        "Certificate with serial number " +
                        cert.getSerialNumber() +
                        " and subject '" +
                        cert.getSubjectX500Principal() +
                        "' is revoked."
                    );
                }
            }
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only the certificates deemed advertisable at {@link #refresh(KeyStore, Predicate)} time are returned, since
     * this list is what the JSSE stack sends as {@code certificate_authorities} to <b>every</b> client performing a
     * handshake on the listener. Returning the whole trust store here would disclose the identity of every accepted
     * client certificate and make the handshake grow with the number of registered certificates.
     * </p>
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return trustMaterial.acceptedIssuers().toArray(new X509Certificate[0]);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkClientTrusted(chain, authType, engine);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        X509ExtendedTrustManager trustManager = this.trustMaterial.trustManager();
        checkRevoked(chain);
        if (trustManager != null) {
            trustManager.checkServerTrusted(chain, authType, engine);
        }
    }
}
