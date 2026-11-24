package io.gravitee.node.vertx.client.ssl;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.TrustOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Accept any server certificate without the Vert.x {@code trustAll} flag.
 * <p>
 * The Vert.x trust-all trust manager is not an {@link javax.net.ssl.X509ExtendedTrustManager}: since 4.2.17, Netty
 * refuses it with OpenSSL on Java 25 as soon as an endpoint identification algorithm is configured, which Netty 4.2 does
 * by default and Vert.x 5.0 does not override. The Netty {@link InsecureTrustManagerFactory} one is extended, and accepted
 * in every case.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TrustAllTrustOptions {

    /**
     * Make the client accept any server certificate. Callers also turn hostname verification off: with any certificate
     * accepted, it adds nothing.
     */
    public static void applyTo(ClientOptionsBase options) {
        options.setTrustAll(false);
        options.setTrustOptions(TrustOptions.wrap(InsecureTrustManagerFactory.INSTANCE.getTrustManagers()[0]));
    }
}
