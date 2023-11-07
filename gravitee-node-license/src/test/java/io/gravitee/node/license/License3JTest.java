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
package io.gravitee.node.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.license.license3j.License3J;
import io.gravitee.node.license.license3j.License3JFeature;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import javax0.license3j.Feature;
import javax0.license3j.License;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey Haeyaert (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
public class License3JTest {

    private static final String FEATURE_NAME = "test-feature-name";
    private static final String FEATURE_VALUE = "test-feature-value";

    @Mock
    private License license;

    @Mock
    private Feature feature;

    private License3J cut;

    @BeforeEach
    void init() {
        cut = new License3J(license);
    }

    @Test
    void should_return_feature_from_license() {
        when(license.get(FEATURE_NAME)).thenReturn(feature);

        final Optional<License3JFeature> feature = cut.feature(FEATURE_NAME);
        assertThat(feature).isNotEmpty();
    }

    @Test
    void should_return_empty_feature_from_license_when_unknown() {
        when(license.get(FEATURE_NAME)).thenReturn(null);

        final Optional<License3JFeature> feature = cut.feature(FEATURE_NAME);
        assertThat(feature).isEmpty();
    }

    @Test
    void should_return_all_features_from_license() {
        final Map<String, Feature> allFeatures = createFeatures();
        when(license.getFeatures()).thenReturn(allFeatures);

        final Map<String, Object> features = cut.features();
        assertThat(features).hasSize(allFeatures.size());
    }

    @Test
    void should_return_all_features_as_string_from_license() {
        final Map<String, Feature> allFeatures = createFeatures();
        when(license.getFeatures()).thenReturn(allFeatures);

        final Map<String, String> features = cut.featuresAsString();
        assertThat(features).hasSize(allFeatures.size());
    }

    @Test
    void should_validate_license() throws InvalidLicenseException {
        when(license.isOK(any(byte[].class))).thenReturn(true);
        when(license.isExpired()).thenReturn(false);

        cut.verify();
    }

    @Test
    void should_throw_invalid_license_when_license_is_not_ok() {
        when(license.isOK(any(byte[].class))).thenReturn(false);

        assertThrows(InvalidLicenseException.class, () -> cut.verify());
    }

    @Test
    void should_throw_invalid_license_when_license_is_expired() {
        when(license.isOK(any(byte[].class))).thenReturn(true);
        when(license.isExpired()).thenReturn(true);

        assertThrows(InvalidLicenseException.class, () -> cut.verify());
    }

    private Map<String, Feature> createFeatures() {
        final Map<String, Feature> allFeatures = new HashMap<>();

        allFeatures.put(FEATURE_NAME + "-big-decimal", Feature.Create.bigDecimalFeature(FEATURE_VALUE + "-big-decimal", new BigDecimal(0)));
        allFeatures.put(
            FEATURE_NAME + "-big-integer",
            Feature.Create.bigIntegerFeature(FEATURE_VALUE + "-big-integer", new BigInteger("0"))
        );
        allFeatures.put(FEATURE_NAME + "-binary", Feature.Create.binaryFeature(FEATURE_VALUE + "-binary", new byte[0]));
        allFeatures.put(FEATURE_NAME + "-byte", Feature.Create.byteFeature(FEATURE_VALUE + "-byte", (byte) 0x00));
        allFeatures.put(FEATURE_NAME + "-date", Feature.Create.dateFeature(FEATURE_VALUE + "-date", new Date()));
        allFeatures.put(FEATURE_NAME + "-double", Feature.Create.doubleFeature(FEATURE_VALUE + "-double", 0.0d));
        allFeatures.put(FEATURE_NAME + "-float", Feature.Create.floatFeature(FEATURE_VALUE + "-float", 0.0f));
        allFeatures.put(FEATURE_NAME + "-int", Feature.Create.intFeature(FEATURE_VALUE + "-int", 0));
        allFeatures.put(FEATURE_NAME + "-long", Feature.Create.longFeature(FEATURE_VALUE + "-long", 0L));
        allFeatures.put(FEATURE_NAME + "-short", Feature.Create.shortFeature(FEATURE_VALUE + "-short", (short) 0));
        allFeatures.put(FEATURE_NAME + "-string", Feature.Create.stringFeature(FEATURE_VALUE + "-string", "0"));
        allFeatures.put(
            FEATURE_NAME + "-uuid",
            Feature.Create.uuidFeature(FEATURE_VALUE + "-uuid", UUID.fromString("00000000-0000-0000-0000-000000000000"))
        );

        return allFeatures;
    }
}
