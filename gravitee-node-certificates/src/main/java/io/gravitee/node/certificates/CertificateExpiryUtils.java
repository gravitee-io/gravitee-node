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
package io.gravitee.node.certificates;

import io.gravitee.common.util.KeyStoreUtils;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Utility for checking X.509 certificate expiry and logging actionable warnings.
 *
 * @author GraviteeSource Team
 */
public final class CertificateExpiryUtils {

    static final int EXPIRY_WARNING_THRESHOLD_DAYS = 30;

    private CertificateExpiryUtils() {}

    /**
     * Inspects every certificate entry in the given {@link KeyStore} and logs a warning when a
     * certificate has expired, is not yet valid, or will expire within {@value #EXPIRY_WARNING_THRESHOLD_DAYS} days.
     */
    public static void warnIfExpired(KeyStore keyStore, Logger log) {
        try {
            for (String alias : Collections.list(keyStore.aliases())) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate x509) {
                    checkCertificate(alias, x509, log);
                }
            }
        } catch (KeyStoreException e) {
            log.warn("Unable to inspect keystore for certificate expiry", e);
        }
    }

    /**
     * Inspects a collection of certificates (e.g. parsed from a PEM stream) and logs expiry warnings.
     * The {@code source} label is used in log messages to identify where the certificates come from.
     */
    public static void warnIfExpired(Collection<? extends Certificate> certificates, String source, Logger log) {
        int index = 0;
        for (Certificate cert : certificates) {
            if (cert instanceof X509Certificate x509) {
                String identifier = buildIdentifier(source, index, x509);
                checkCertificate(identifier, x509, log);
            }
            index++;
        }
    }

    /**
     * Materializes a JKS or PKCS12 store from a file path or base64-encoded content, then logs expiry
     * warnings for its certificates. Best-effort: any materialization failure is logged at debug level
     * so that an inspection problem can never prevent the caller from proceeding.
     *
     * @param type the keystore type (e.g. {@code JKS}, {@code PKCS12})
     * @param source a human-readable label used in log messages when a failure occurs
     */
    public static void inspectKeyStore(String type, String path, String base64Content, String password, String source, Logger log) {
        try {
            KeyStore keyStore = null;
            if (path != null && !path.isEmpty()) {
                keyStore = KeyStoreUtils.initFromPath(type, path, password);
            } else if (base64Content != null && !base64Content.isEmpty()) {
                keyStore = KeyStoreUtils.initFromContent(type, base64Content, password);
            }
            if (keyStore != null) {
                warnIfExpired(keyStore, log);
            }
        } catch (RuntimeException e) {
            log.debug("Unable to inspect {} certificate expiry", source, e);
        }
    }

    /**
     * Parses PEM certificate material provided as file paths and/or inline content, then logs expiry
     * warnings. Best-effort: parsing failures are logged at debug level.
     *
     * @param source a human-readable label used to identify the certificates in log messages
     */
    public static void inspectPem(List<String> paths, List<String> contents, String source, Logger log) {
        try {
            if (paths != null) {
                final var keystore = KeyStoreUtils.initFromPemCertificateFiles(
                    paths.stream().filter(Objects::nonNull).toList(),
                    UUID.randomUUID().toString()
                );
                warnIfExpired(keystore, log);
            }
            if (contents != null) {
                for (String content : contents) {
                    if (content != null && !content.isEmpty()) {
                        warnIfExpired(List.of(KeyStoreUtils.loadPemCertificates(content)), source, log);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Unable to inspect {} certificate expiry", source, e);
        }
    }

    private static String buildIdentifier(String source, int index, X509Certificate x509) {
        String dn = x509.getSubjectX500Principal().getName();
        return dn.isEmpty() ? source + "[" + index + "]" : source + " - " + dn;
    }

    private static void checkCertificate(String identifier, X509Certificate cert, Logger log) {
        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            log.error("Certificate '{}' has EXPIRED on {}", identifier, cert.getNotAfter());
            return;
        } catch (CertificateNotYetValidException e) {
            log.warn("Certificate '{}' is not yet valid (valid from {})", identifier, cert.getNotBefore());
            return;
        }
        long daysLeft = ChronoUnit.DAYS.between(Instant.now(), cert.getNotAfter().toInstant());
        if (daysLeft <= EXPIRY_WARNING_THRESHOLD_DAYS) {
            log.warn("Certificate '{}' expires in {} day(s) on {}", identifier, daysLeft, cert.getNotAfter());
        }
    }
}
