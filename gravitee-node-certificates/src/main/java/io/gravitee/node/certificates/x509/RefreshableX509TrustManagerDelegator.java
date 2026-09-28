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
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
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

    /**
     * Verdicts that stay fatal even where an unknown client certificate is tolerated, because reaching them means
     * the trust anchors could actually judge the certificate.
     */
    private static final Set<CertPathValidatorException.Reason> FATAL_REASONS = Set.of(
        CertPathValidatorException.BasicReason.EXPIRED,
        CertPathValidatorException.BasicReason.NOT_YET_VALID,
        CertPathValidatorException.BasicReason.REVOKED,
        CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS
    );

    /** Deep enough for any JSSE chain (validator wraps path validator wraps the certificate exception). */
    private static final int MAX_CAUSE_DEPTH = 16;

    private final String target;
    private final boolean failOnUntrustedClientCertificate;
    private volatile List<CRL> crls = List.of();

    /**
     * The trust manager and the certificates sent as {@code certificate_authorities} always come from the same
     * refresh, so a handshake can never combine one with the other's. The sent certificates are deliberately
     * <b>not</b> the full set of trust anchors: entries registered dynamically at runtime (typically
     * per-subscription client certificates) must be trusted for validation, but must never reach the wire.
     */
    private record TrustMaterial(X509ExtendedTrustManager trustManager, List<X509Certificate> acceptedIssuers, boolean hasTrustAnchors) {}

    private static final TrustMaterial EMPTY = new TrustMaterial(null, List.of(), false);

    private volatile TrustMaterial trustMaterial = EMPTY;

    public RefreshableX509TrustManagerDelegator(String target) {
        this(target, true);
    }

    /**
     * @param failOnUntrustedClientCertificate {@code false} lets a client certificate that no trust anchor can judge
     *        go through instead of breaking the handshake. This is meant for a listener that merely
     *        <em>requests</em> a certificate and has no configured trust store: presenting one is optional there,
     *        so refusing an unknown one would reject callers that were never asked to authenticate this way.
     *        Deciding who the certificate belongs to is then the application's job.
     *        <p>
     *        Only "I do not know this certificate" is downgraded, which is also what an expired certificate that
     *        chains to nothing amounts to. A verdict the trust anchors were able to reach stays fatal: a
     *        certificate that chains to a registered authority and is expired, not yet valid or revoked. Server
     *        certificates are never affected by this flag.
     */
    public RefreshableX509TrustManagerDelegator(String target, boolean failOnUntrustedClientCertificate) {
        this.target = Objects.requireNonNull(target, "target cannot be null");
        this.failOnUntrustedClientCertificate = failOnUntrustedClientCertificate;
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
            Anchors anchors = collectAnchors(keyStore, advertisableAlias);
            // one volatile write, so a handshake never combines a new trust manager with a stale issuer list
            this.trustMaterial = new TrustMaterial(newTrustManager, anchors.advertisable(), anchors.anyAnchor());

            log.info(
                "Trust store has been (re)loaded with {} entries ({} sent as client certificate authorities) for target: {}",
                keyStore.size(),
                anchors.advertisable().size(),
                target
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create trust manager for target: %s".formatted(target), e);
        }
    }

    /**
     * The anchors the JDK derives from a store, and which of them may be advertised, in one walk.
     *
     * @param advertisable the certificates matching {@code advertisableAlias}, sent as {@code certificate_authorities}
     * @param anyAnchor whether the store yields any trust anchor at all, which is not the same as holding entries
     */
    private record Anchors(List<X509Certificate> advertisable, boolean anyAnchor) {}

    /**
     * Mirrors {@code sun.security.validator.TrustStoreUtil#getTrustedCerts(KeyStore)}, which is what the JDK trust
     * manager takes as trust anchors. Key entries count through the first certificate of their chain, and duplicates
     * are collapsed, exactly as the JDK does. Aliases the caller does not allow to advertise are still anchors, they
     * are only kept out of the returned list.
     */
    private static Anchors collectAnchors(KeyStore keyStore, Predicate<String> advertisableAlias) throws KeyStoreException {
        Set<X509Certificate> advertisable = new LinkedHashSet<>();
        boolean anyAnchor = false;
        for (String alias : Collections.list(keyStore.aliases())) {
            X509Certificate anchor = anchorOf(keyStore, alias);
            if (anchor == null) {
                continue;
            }
            anyAnchor = true;
            if (advertisableAlias.test(alias)) {
                advertisable.add(anchor);
            }
        }
        return new Anchors(List.copyOf(advertisable), anyAnchor);
    }

    private static X509Certificate anchorOf(KeyStore keyStore, String alias) throws KeyStoreException {
        if (keyStore.isCertificateEntry(alias)) {
            return asX509(keyStore.getCertificate(alias));
        }
        if (keyStore.isKeyEntry(alias)) {
            Certificate[] chain = keyStore.getCertificateChain(alias);
            return chain != null && chain.length > 0 ? asX509(chain[0]) : null;
        }
        return null;
    }

    private static X509Certificate asX509(Certificate certificate) {
        return certificate instanceof X509Certificate x509Certificate ? x509Certificate : null;
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
        checkClient(chain, trustManager -> trustManager.checkClientTrusted(chain, authType));
    }

    /**
     * Runs a client certificate check, revocation first, and decides what an unknown certificate means for this
     * target.
     * <p>
     * Revocation against the CRLs this class holds is refused unconditionally: such a certificate was trusted once
     * and explicitly taken back, which is a different statement from "unknown here". Of the delegated check's own
     * verdicts, those listed in {@link #FATAL_REASONS} stay fatal too, since reaching them means the anchors could
     * judge the certificate. With nothing loaded, or with no trust anchor on a target that tolerates unknown
     * certificates, no delegated check runs at all.
     */
    private void checkClient(X509Certificate[] chain, ClientCertificateCheck check) throws CertificateException {
        TrustMaterial material = this.trustMaterial;
        checkRevoked(chain);
        if (material.trustManager() == null) {
            return;
        }
        if (!failOnUntrustedClientCertificate && !material.hasTrustAnchors()) {
            // An anchorless trust manager refuses everyone, which is not what an empty store means here: nothing
            // has been registered yet, or nothing is registered any more. Skipping the check keeps the listener in
            // the state it had before the first registration instead of flipping it to "refuse everyone".
            return;
        }
        try {
            check.run(material.trustManager());
        } catch (CertificateException e) {
            if (failOnUntrustedClientCertificate || isFatalVerdict(e)) {
                throw e;
            }
            log.debug(
                "Client certificate '{}' is not trusted by target: {}, letting the handshake complete since presenting a certificate is optional there",
                chain.length > 0 ? chain[0].getSubjectX500Principal() : "none",
                target,
                e
            );
        }
    }

    /**
     * Tells a verdict the trust anchors were able to reach apart from "I do not know this certificate".
     * <p>
     * A failure found while <em>validating</em> a certification path comes wrapped: the JDK reports it as a
     * {@link CertPathValidatorException} carrying a {@link CertPathValidatorException.BasicReason}, itself wrapped
     * in the {@code CertificateException} the trust manager throws. Failing to <em>build</em> a path, on the other
     * hand, carries no reason and no cause: nothing chains to a trust anchor, which is exactly the unknown
     * certificate this target tolerates. Revocation is listed here too, because the JDK runs its own revocation
     * checks inside the delegated call when {@code com.sun.net.ssl.checkRevocation} is on, where the CRLs this
     * class holds would not see it.
     */
    private static boolean isFatalVerdict(Throwable throwable) {
        // bounded: an exception chain should not cycle, and one that does must not hang a handshake
        Throwable cause = throwable;
        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++, cause = cause.getCause()) {
            if (cause instanceof CertificateExpiredException || cause instanceof CertificateNotYetValidException) {
                return true;
            }
            if (
                cause instanceof CertPathValidatorException pathValidatorException &&
                FATAL_REASONS.contains(pathValidatorException.getReason())
            ) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    private interface ClientCertificateCheck {
        void run(X509ExtendedTrustManager trustManager) throws CertificateException;
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
        checkClient(chain, trustManager -> trustManager.checkClientTrusted(chain, authType, socket));
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
        checkClient(chain, trustManager -> trustManager.checkClientTrusted(chain, authType, engine));
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
