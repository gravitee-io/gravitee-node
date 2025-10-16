#!/bin/bash
#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

cd "$(dirname "$0")"

# Generate CA
openssl req -newkey rsa:2048 -keyform PEM -keyout ca.key -x509 -days 36500 -subj "/C=FR/ST=Test/L=Test/O=Gravitee/CN=Test CA" -passout pass:secret -outform PEM -out ca.pem 2>/dev/null

# Create CA truststore
rm -f ca-truststore.jks
keytool -import -trustcacerts -alias test-ca -file ca.pem -keystore ca-truststore.jks -storepass secret -noprompt >/dev/null 2>&1

# Generate client certificate to be revoked
openssl genrsa -out cert-client-revoked.key 2048 2>/dev/null
openssl req -new -key cert-client-revoked.key -out cert-client-revoked.csr -subj "/C=FR/ST=France/L=Lille/O=GraviteeSource/OU=GraviteeSource/CN=revoked-client" 2>/dev/null
openssl x509 -req -in cert-client-revoked.csr -CA ca.pem -CAkey ca.key -CAcreateserial -out cert-client-revoked.pem -days 36500 -sha256 -passin pass:secret 2>/dev/null

# Generate valid client certificate (not revoked)
openssl genrsa -out cert-client-valid.key 2048 2>/dev/null
openssl req -new -key cert-client-valid.key -out cert-client-valid.csr -subj "/C=FR/ST=France/L=Paris/O=GraviteeSource/OU=GraviteeSource/CN=valid-client" 2>/dev/null
openssl x509 -req -in cert-client-valid.csr -CA ca.pem -CAkey ca.key -CAcreateserial -out cert-client-valid.pem -days 36500 -sha256 -passin pass:secret 2>/dev/null

# Create OpenSSL CA configuration
cat > ca.conf << 'EOF'
[ ca ]
default_ca = CA_default

[ CA_default ]
dir              = .
certs            = $dir
crl_dir          = $dir
new_certs_dir    = $dir
database         = $dir/index.txt
serial           = $dir/serial
RANDFILE         = $dir/.rand
private_key      = $dir/ca.key
certificate      = $dir/ca.pem
crl              = $dir/crl.pem
crl_extensions   = crl_ext
default_crl_days = 36500
default_md       = sha256
preserve         = no
policy           = policy_anything

[ policy_anything ]
countryName            = optional
stateOrProvinceName    = optional
localityName           = optional
organizationName       = optional
organizationalUnitName = optional
commonName             = supplied
emailAddress           = optional

[ crl_ext ]
authorityKeyIdentifier=keyid:always
EOF

# Initialize CA database
touch index.txt
echo '01' > serial
echo '01' > crlnumber

# Generate empty CRLs
openssl ca -config ca.conf -gencrl -out crl-empty.pem -crldays 36500 -passin pass:secret 2>/dev/null

# Revoke certificate and generate CRLs with revocations
openssl ca -config ca.conf -revoke cert-client-revoked.pem -passin pass:secret 2>/dev/null
openssl ca -config ca.conf -gencrl -out crl-with-revocations.pem -crldays 36500 -passin pass:secret 2>/dev/null
openssl crl -in crl-with-revocations.pem -outform DER -out crl-with-revocations.der 2>/dev/null

# Cleanup
rm -f ca.conf index.txt* serial* crlnumber* .rand ca.key cert-client-revoked.key cert-client-revoked.csr cert-client-valid.key cert-client-valid.csr ca.srl

echo "CRLs regenerated successfully (valid for 100 years)"
