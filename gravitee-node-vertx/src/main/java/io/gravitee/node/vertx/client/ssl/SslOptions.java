package io.gravitee.node.vertx.client.ssl;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
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
    private boolean hostnameVerifier = true;

    @Builder.Default
    private boolean openSsl = false;

    @Builder.Default
    private String hostnameVerificationAlgorithm = "NONE";

    private TrustStore trustStore;
    private KeyStore keyStore;

    public Optional<TrustStore> trustStore() {
        return Optional.ofNullable(trustStore);
    }

    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(keyStore);
    }
}
