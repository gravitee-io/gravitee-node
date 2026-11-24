package io.gravitee.node.vertx.client.ssl;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SslOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = 5578794192878572915L;

    private boolean trustAll;

    @Builder.Default
    private boolean hostnameVerifier = true;

    @Builder.Default
    private boolean openSsl = false;

    @Builder.Default
    private boolean alpn = false;

    @Builder.Default
    private String hostnameVerificationAlgorithm = "NONE";

    private Set<String> tlsProtocols;
    private List<String> tlsCiphers;

    private TrustStore trustStore;
    private KeyStore keyStore;

    public Optional<TrustStore> trustStore() {
        return Optional.ofNullable(trustStore);
    }

    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(keyStore);
    }

    /**
     * Hostname verification algorithm to apply on the Vert.x
     * {@code NetClientOptions}. Precedence:
     * <ol>
     *   <li>{@link #trustAll} — always {@code ""} (disabled), whatever
     *       {@link #hostnameVerifier} and {@link #hostnameVerificationAlgorithm}
     *       say: any certificate is accepted, so checking its hostname adds
     *       nothing. Configure a truststore instead to keep the check.</li>
     *   <li>{@link #hostnameVerificationAlgorithm} if set and not
     *       {@code "NONE"} — used verbatim, supports {@code HTTPS},
     *       {@code LDAPS}, etc.</li>
     *   <li>Otherwise {@link #hostnameVerifier} — {@code true} →
     *       {@code "HTTPS"}, {@code false} → {@code ""} (disabled).</li>
     * </ol>
     * Never returns {@code null}, so it is safe to pass directly to
     * {@code NetClientOptions#setHostnameVerificationAlgorithm}.
     */
    public String effectiveHostnameVerificationAlgorithm() {
        if (trustAll) {
            return "";
        }
        if (
            hostnameVerificationAlgorithm != null &&
            !hostnameVerificationAlgorithm.isEmpty() &&
            !"NONE".equalsIgnoreCase(hostnameVerificationAlgorithm)
        ) {
            return hostnameVerificationAlgorithm;
        }
        return hostnameVerifier ? "HTTPS" : "";
    }

    /**
     * Whether an HTTP client must verify the server hostname. Never with
     * {@link #trustAll}, whatever {@link #hostnameVerifier} says: any certificate is
     * accepted, so checking its hostname adds nothing, and Netty 4.2.17+ refuses the
     * handshake on Java 25 with OpenSSL when Vert.x combines both. Configure a
     * truststore instead to keep the check.
     */
    public boolean effectiveVerifyHost() {
        return hostnameVerifier && !trustAll;
    }
}
