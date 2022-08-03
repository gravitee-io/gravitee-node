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
package io.gravitee.node.vertx.metrics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class RenameVertxFilterTest {

    @Mock
    private Meter.Id meterId;

    private RenameVertxFilter cut;

    @Test
    void shouldRenameVertxPrefixedMetrics() {
        cut = new RenameVertxFilter();

        when(meterId.withName(any(String.class))).thenReturn(meterId);
        when(meterId.getName()).thenReturn("vertx.testCategory");

        cut.map(meterId);

        verify(meterId).withName("testCategory");
    }

    @Test
    void shouldNotRenameNotVertxPrefixedMetrics() {
        cut = new RenameVertxFilter();

        when(meterId.getName()).thenReturn("test.Category");

        cut.map(meterId);

        verify(meterId, never()).withName(any(String.class));
    }
}
