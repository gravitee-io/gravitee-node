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
package io.gravitee.node.vertx.server.tcp;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Throwaway PKI for the tests: a CA and a server certificate, written as PKCS12 stores. */
final class TestPki {

    static final char[] PWD = "secret".toCharArray();
    private static long serial = System.nanoTime();

    record Ident(KeyPair keys, X509Certificate cert) {}

    static Ident ca(String cn) throws Exception {
        KeyPair kp = rsa();
        X500Name n = new X500Name("CN=" + cn);
        var b = new JcaX509v3CertificateBuilder(n, next(), from(), to(), n, kp.getPublic());
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        return new Ident(kp, new JcaX509CertificateConverter().getCertificate(b.build(signer(kp))));
    }

    static Ident leaf(Ident ca, String cn, String san) throws Exception {
        KeyPair kp = rsa();
        var b = new JcaX509v3CertificateBuilder(
            new X500Name(ca.cert.getSubjectX500Principal().getName()),
            next(),
            from(),
            to(),
            new X500Name("CN=" + cn),
            kp.getPublic()
        );
        b.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, san)));
        return new Ident(kp, new JcaX509CertificateConverter().getCertificate(b.build(signer(ca.keys))));
    }

    static void writeKeyStore(Path file, Ident ca, Ident leaf) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("server", leaf.keys.getPrivate(), PWD, new X509Certificate[] { leaf.cert, ca.cert });
        store(ks, file);
    }

    static void writeTrustStore(Path file, Ident ca) throws Exception {
        store(trustOf(ca), file);
    }

    static KeyStore trustOf(Ident ca) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setCertificateEntry("ca", ca.cert);
        return ks;
    }

    private static void store(KeyStore ks, Path file) throws Exception {
        try (OutputStream o = Files.newOutputStream(file)) {
            ks.store(o, PWD);
        }
    }

    private static org.bouncycastle.operator.ContentSigner signer(KeyPair kp) throws Exception {
        return new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
    }

    private static KeyPair rsa() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    private static BigInteger next() {
        return BigInteger.valueOf(serial++);
    }

    private static Date from() {
        return new Date(System.currentTimeMillis() - 3_600_000L);
    }

    private static Date to() {
        return new Date(System.currentTimeMillis() + 86_400_000L * 30);
    }
}
