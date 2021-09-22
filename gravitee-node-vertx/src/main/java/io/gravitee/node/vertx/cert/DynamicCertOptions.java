package io.gravitee.node.vertx.cert;

import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.function.Function;


public class DynamicCertOptions implements KeyCertOptions {

    private static final Logger logger = LoggerFactory.getLogger(DynamicCertOptions.class);
    private final MyKeyManager keyManager = new MyKeyManager();
    private final MyKeyManagerFactory factory = new MyKeyManagerFactory(keyManager);

    /**
     * Dynamically load the given keystore, replacing any previously loaded keystore.
     */
    public void load(String defaultAlias, KeyStore ks, char[] passwd) throws UnrecoverableKeyException, KeyStoreException {
        keyManager.load(defaultAlias, ks, passwd);
    }

    @Override
    public KeyCertOptions copy() {
        return null;
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory(Vertx vertx) throws Exception {
        return factory;
    }

    @Override
    public Function<String, X509KeyManager> keyManagerMapper(Vertx vertx) throws Exception {
        return null;
    }


    static class MyKeyManager extends X509ExtendedKeyManager {

        private String defaultAlias;
        private volatile X509ExtendedKeyManager wrapped;

        public void load(String defaultAlias, KeyStore ks, char[] passwd) throws UnrecoverableKeyException, KeyStoreException {
            try {
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(ks, passwd);
                this.defaultAlias = defaultAlias;
                wrapped = (X509ExtendedKeyManager) keyManagerFactory.getKeyManagers()[0];
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine engine) {
            /*
            From https://github.com/grahamedgecombe/netty-sni-example/blob/4c1b5b17e06c9478243617979b6e5e3f0d7103ff/src/main/java/SniKeyManager.java

            Copyright (c) 2014 Graham Edgecombe <graham@grahamedgecombe.com>

            Permission to use, copy, modify, and/or distribute this software for any
            purpose with or without fee is hereby granted, provided that the above
            copyright notice and this permission notice appear in all copies.

            THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
            WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
            MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
            ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
            WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
            ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
            OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
             */
            ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();

            // Pick first SNIHostName in the list of SNI names.
            String hostname = null;
            for (SNIServerName name : session.getRequestedServerNames()) {
                if (name.getType() == StandardConstants.SNI_HOST_NAME) {
                    hostname = ((SNIHostName) name).getAsciiName();
                    break;
                }
            }

            logger.trace("Hostname = " + hostname);

            // If we got given a hostname over SNI, check if we have a cert and key for that hostname. If so, we use it.
            // Otherwise, we fall back to the default certificate.
            if (hostname != null && (getCertificateChain(hostname) != null && getPrivateKey(hostname) != null))
                return hostname;

            return defaultAlias;
        }

        @Override
        public String[] getServerAliases(String s, Principal[] principals) {
            return wrapped == null ? null : wrapped.getServerAliases(s, principals);
        }

        @Override
        public X509Certificate[] getCertificateChain(String s) {
            return wrapped == null ? null : wrapped.getCertificateChain(s);
        }

        @Override
        public PrivateKey getPrivateKey(String s) {
            return wrapped == null ? null : wrapped.getPrivateKey(s);
        }

        @Override
        public String[] getClientAliases(String s, Principal[] principals) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
            throw new UnsupportedOperationException();
        }
    }

    static class MyKeyManagerFactory extends KeyManagerFactory {
        MyKeyManagerFactory(final KeyManager keyManager) {
            super(new KeyManagerFactorySpi() {
                @Override
                protected void engineInit(KeyStore keyStore, char[] chars) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws InvalidAlgorithmParameterException {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected KeyManager[] engineGetKeyManagers() {
                    return new KeyManager[]{ keyManager };
                }
            }, new Provider("", 0.0, "") {
            }, "");
        }
    }
}
