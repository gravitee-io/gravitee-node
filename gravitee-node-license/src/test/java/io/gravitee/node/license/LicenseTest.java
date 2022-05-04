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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.Feature;
import io.gravitee.node.api.license.License;
import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LicenseTest {

    private static final String FEATURE_NAME = "test-feature-name";

    private Node node = Mockito.spy(new TestNode());

    @Mock
    private License licenseMock = Mockito.mock(License.class);

    @Mock
    private Feature featureMock = Mockito.mock(Feature.class);

    @Before
    public void setup() {
        when(node.license()).thenReturn(licenseMock);
    }

    @Test
    public void isLicenseIncluded_should_return_false_if_license_feature_is_null() {
        when(licenseMock.feature(FEATURE_NAME)).thenReturn(null);

        assertFalse(node.license().isFeatureIncluded(FEATURE_NAME));
    }

    @Test
    public void isLicenseIncluded_should_return_false_if_license_feature_is_not_included() {
        when(licenseMock.feature(FEATURE_NAME)).thenReturn(Optional.of(featureMock));
        when(featureMock.getString()).thenReturn("something-other-than-included");
        assertFalse(node.license().isFeatureIncluded(FEATURE_NAME));
    }

    @Test
    @Ignore
    public void isLicenseIncluded_should_return_true_if_license_feature_is_included() {
        when(licenseMock.feature(FEATURE_NAME)).thenReturn(Optional.of(featureMock));
        when(featureMock.getString()).thenReturn("included");
        assertTrue(node.license().isFeatureIncluded(FEATURE_NAME));
    }

    private class TestNode implements Node {

        @Override
        public String application() {
            return "test-node";
        }

        @Override
        public String name() {
            return "test-node";
        }

        @Override
        public Lifecycle.State lifecycleState() {
            return null;
        }

        @Override
        public Node start() throws Exception {
            return null;
        }

        @Override
        public Node stop() throws Exception {
            return null;
        }
    }
}
