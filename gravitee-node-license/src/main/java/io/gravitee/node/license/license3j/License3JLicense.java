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

import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class License3JLicense implements License {

    private final javax0.license3j.License license;

    public License3JLicense(javax0.license3j.License license) {
        this.license = license;
    }

    @Override
    public Optional<Feature> feature(String name) {
        javax0.license3j.Feature feature = (license == null) ? null : license.get(name);
        return (feature == null) ? Optional.empty() : Optional.of(new License3JFeature(feature));
    }

    @Override
    public Map<String, Object> features() {
        return license
            .getFeatures()
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    new Function<Map.Entry<String, javax0.license3j.Feature>, Object>() {
                        @Override
                        public Object apply(Map.Entry<String, javax0.license3j.Feature> entry) {
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
                            return null;
                        }
                    }
                )
            );
    }
}
