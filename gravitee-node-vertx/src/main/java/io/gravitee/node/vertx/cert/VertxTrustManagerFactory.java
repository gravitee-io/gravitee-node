package io.gravitee.node.vertx.cert;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.util.Objects;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
class VertxTrustManagerFactory extends TrustManagerFactory {

    private static final String TRUST_MANAGER_FACTORY_ALGORITHM = "no-algorithm";
    private static final Provider PROVIDER = new Provider("", "1.0", "") {};

    VertxTrustManagerFactory(TrustManager trustManager) {
        super(new TrustManagerFactorySpiWrapper(trustManager), PROVIDER, TRUST_MANAGER_FACTORY_ALGORITHM);
    }

    private static class TrustManagerFactorySpiWrapper extends TrustManagerFactorySpi {

        private final TrustManager[] trustManagers;

        private TrustManagerFactorySpiWrapper(TrustManager trustManager) {
            Objects.requireNonNull(trustManager);
            this.trustManagers = new TrustManager[] { trustManager };
        }

        @Override
        protected void engineInit(KeyStore ks) throws KeyStoreException {
            log.info("Ignoring provided KeyStore");
        }

        @Override
        protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
            log.info("Ignoring provided ManagerFactoryParameters");
        }

        @Override
        protected TrustManager[] engineGetTrustManagers() {
            return trustManagers;
        }
    }
}
