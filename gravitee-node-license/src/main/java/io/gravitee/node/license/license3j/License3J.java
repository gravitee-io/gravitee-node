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
package io.gravitee.node.license.license3j;

import io.gravitee.node.api.license.ExpiredLicenseException;
import io.gravitee.node.api.license.InvalidLicenseException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class License3J {

    public static final String LICENSE_EXPIRE_AT = "expiryDate";

    // prettier-ignore
    private static final byte[] key = new byte[]{
            (byte) 0x52,
            (byte) 0x53, (byte) 0x41, (byte) 0x00, (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x22, (byte) 0x30,
            (byte) 0x0D, (byte) 0x06, (byte) 0x09, (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x0D, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x05, (byte) 0x00, (byte) 0x03, (byte) 0x82,
            (byte) 0x01, (byte) 0x0F, (byte) 0x00, (byte) 0x30, (byte) 0x82, (byte) 0x01, (byte) 0x0A, (byte) 0x02,
            (byte) 0x82, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0xD8, (byte) 0x59, (byte) 0xEC, (byte) 0xB6,
            (byte) 0x27, (byte) 0xF2, (byte) 0x20, (byte) 0x2F, (byte) 0x7A, (byte) 0x39, (byte) 0x86, (byte) 0x2B,
            (byte) 0x62, (byte) 0xB6, (byte) 0xEA, (byte) 0x5B, (byte) 0xE4, (byte) 0x80, (byte) 0xA0, (byte) 0x32,
            (byte) 0x35, (byte) 0xB3, (byte) 0xC8, (byte) 0xD5, (byte) 0x4E, (byte) 0xB7, (byte) 0xA1, (byte) 0xFE,
            (byte) 0x15, (byte) 0x84, (byte) 0xC7, (byte) 0x75, (byte) 0x66, (byte) 0x8F, (byte) 0x48, (byte) 0xF1,
            (byte) 0xD6, (byte) 0x30, (byte) 0x7B, (byte) 0x39, (byte) 0xB7, (byte) 0xD7, (byte) 0x48, (byte) 0x4F,
            (byte) 0xAF, (byte) 0x38, (byte) 0xC6, (byte) 0xB9, (byte) 0xBC, (byte) 0x3C, (byte) 0xEB, (byte) 0xED,
            (byte) 0xB3, (byte) 0x03, (byte) 0xEE, (byte) 0x1B, (byte) 0x9D, (byte) 0x85, (byte) 0x8B, (byte) 0xFC,
            (byte) 0x93, (byte) 0x56, (byte) 0x5C, (byte) 0x09, (byte) 0x91, (byte) 0x41, (byte) 0x22, (byte) 0xE7,
            (byte) 0x4C, (byte) 0xF6, (byte) 0x94, (byte) 0x9D, (byte) 0xC5, (byte) 0x71, (byte) 0x3C, (byte) 0x2D,
            (byte) 0xE4, (byte) 0x4C, (byte) 0x0E, (byte) 0xD5, (byte) 0x3D, (byte) 0x6F, (byte) 0xD7, (byte) 0x20,
            (byte) 0x6C, (byte) 0xD8, (byte) 0xDD, (byte) 0x12, (byte) 0x4D, (byte) 0xEA, (byte) 0x55, (byte) 0xDD,
            (byte) 0x26, (byte) 0xA3, (byte) 0x25, (byte) 0xE3, (byte) 0x83, (byte) 0x9C, (byte) 0x92, (byte) 0x15,
            (byte) 0xC6, (byte) 0x45, (byte) 0xFE, (byte) 0x9A, (byte) 0x0F, (byte) 0x47, (byte) 0x86, (byte) 0x45,
            (byte) 0x04, (byte) 0xB6, (byte) 0x44, (byte) 0xFC, (byte) 0x01, (byte) 0x86, (byte) 0x2A, (byte) 0xF6,
            (byte) 0xE2, (byte) 0xFD, (byte) 0x37, (byte) 0xF1, (byte) 0xBB, (byte) 0x70, (byte) 0xAD, (byte) 0x15,
            (byte) 0xE0, (byte) 0x7C, (byte) 0xB3, (byte) 0x94, (byte) 0xDE, (byte) 0x2F, (byte) 0xD2, (byte) 0xC4,
            (byte) 0x4F, (byte) 0xCB, (byte) 0xA7, (byte) 0x31, (byte) 0x87, (byte) 0x66, (byte) 0xD0, (byte) 0xF7,
            (byte) 0x5D, (byte) 0x09, (byte) 0x84, (byte) 0x4B, (byte) 0xB7, (byte) 0x4B, (byte) 0xE6, (byte) 0x1C,
            (byte) 0xA7, (byte) 0xD9, (byte) 0xBE, (byte) 0x9E, (byte) 0xAD, (byte) 0xE7, (byte) 0x03, (byte) 0xCC,
            (byte) 0xAB, (byte) 0x04, (byte) 0x4D, (byte) 0xDF, (byte) 0x92, (byte) 0x8E, (byte) 0xC5, (byte) 0xA1,
            (byte) 0x04, (byte) 0xD0, (byte) 0x7F, (byte) 0x89, (byte) 0x71, (byte) 0x2D, (byte) 0x6D, (byte) 0x8F,
            (byte) 0xCC, (byte) 0x1E, (byte) 0x25, (byte) 0x5E, (byte) 0x66, (byte) 0xBF, (byte) 0xA9, (byte) 0xF8,
            (byte) 0x8C, (byte) 0xEF, (byte) 0x8E, (byte) 0x6A, (byte) 0xFA, (byte) 0xCE, (byte) 0xFA, (byte) 0x79,
            (byte) 0xA0, (byte) 0xDE, (byte) 0x72, (byte) 0xCB, (byte) 0xBC, (byte) 0x90, (byte) 0x8E, (byte) 0x29,
            (byte) 0x4C, (byte) 0x40, (byte) 0x05, (byte) 0x61, (byte) 0x5A, (byte) 0x44, (byte) 0x0D, (byte) 0xC7,
            (byte) 0x46, (byte) 0x65, (byte) 0xA4, (byte) 0x2A, (byte) 0xD3, (byte) 0xAA, (byte) 0xEC, (byte) 0x83,
            (byte) 0x78, (byte) 0x52, (byte) 0x04, (byte) 0x2B, (byte) 0xB4, (byte) 0x15, (byte) 0xE3, (byte) 0x66,
            (byte) 0xD3, (byte) 0xB5, (byte) 0xE5, (byte) 0xE7, (byte) 0x04, (byte) 0xB7, (byte) 0xDB, (byte) 0xFC,
            (byte) 0x46, (byte) 0x32, (byte) 0xC5, (byte) 0x71, (byte) 0x93, (byte) 0xDD, (byte) 0xE3, (byte) 0x07,
            (byte) 0xD0, (byte) 0xCD, (byte) 0xD8, (byte) 0x0E, (byte) 0xDE, (byte) 0xC1, (byte) 0xA8, (byte) 0xA6,
            (byte) 0x0F, (byte) 0x45, (byte) 0x8F, (byte) 0x06, (byte) 0x51, (byte) 0xFE, (byte) 0x72, (byte) 0xB7,
            (byte) 0x61, (byte) 0xCA, (byte) 0x29, (byte) 0x67, (byte) 0x02, (byte) 0x03, (byte) 0x01, (byte) 0x00,
            (byte) 0x01,
    };

    public static byte[] publicKey() {
        return key;
    }

    private final javax0.license3j.License license;
    private Boolean valid;

    public License3J(javax0.license3j.License license) {
        this.license = license;
    }

    public Optional<License3JFeature> feature(String name) {
        javax0.license3j.Feature feature = (license == null) ? null : license.get(name);
        return (feature == null) ? Optional.empty() : Optional.of(new License3JFeature(feature));
    }

    public Map<String, Object> features() {
        return license
            .getFeatures()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        javax0.license3j.Feature feature = entry.getValue();
                        if (feature.isBigDecimal()) {
                            return feature.getBigDecimal();
                        } else if (feature.isBigInteger()) {
                            return feature.getBigInteger();
                        } else if (feature.isBinary()) {
                            return feature.getBinary();
                        } else if (feature.isByte()) {
                            return feature.getByte();
                        } else if (feature.isDate()) {
                            return feature.getDate();
                        } else if (feature.isDouble()) {
                            return feature.getDouble();
                        } else if (feature.isFloat()) {
                            return feature.getFloat();
                        } else if (feature.isInt()) {
                            return feature.getInt();
                        } else if (feature.isLong()) {
                            return feature.getLong();
                        } else if (feature.isShort()) {
                            return feature.getShort();
                        } else if (feature.isString()) {
                            return feature.getString();
                        } else if (feature.isUUID()) {
                            return feature.getUUID();
                        }
                        // Unreachable code.
                        return feature.valueString();
                    }
                )
            );
    }

    public Map<String, String> featuresAsString() {
        return license
            .getFeatures()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        javax0.license3j.Feature feature = entry.getValue();
                        return feature.valueString();
                    }
                )
            );
    }

    public void verify() throws InvalidLicenseException {
        if (!isValid()) {
            throw new InvalidLicenseException("License is not valid. Please contact GraviteeSource to ask for a valid license.");
        }

        if (isExpired()) {
            throw new ExpiredLicenseException("License is expired. Please contact GraviteeSource to ask for a renewed license.");
        }
    }

    public boolean isValid() {
        if (valid == null) {
            valid = license.isOK(publicKey());
        }

        return valid;
    }

    public boolean isExpired() {
        return license.isExpired();
    }
}
