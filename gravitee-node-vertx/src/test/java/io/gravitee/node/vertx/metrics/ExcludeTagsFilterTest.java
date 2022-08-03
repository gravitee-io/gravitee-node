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

import static org.junit.jupiter.api.Assertions.assertSame;
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
class ExcludeTagsFilterTest {

    protected static final String CATEGORY = "testCategory";

    @Mock
    private Meter.Id meterId;

    private ExcludeTagsFilter cut;

    @Test
    void shouldExcludeSpecifiedLabels() {
        cut = new ExcludeTagsFilter(CATEGORY, List.of("label1", "label2"));

        when(meterId.getName()).thenReturn(CATEGORY);
        when(meterId.getTagsAsIterable())
            .thenReturn(
                List.of(
                    Tag.of("a", "valueA"),
                    Tag.of("b", "valueB"),
                    Tag.of("label1", "value"),
                    Tag.of("label2", "value"),
                    Tag.of("c", "valueC")
                )
            );

        cut.map(meterId);

        verify(meterId).replaceTags(List.of(Tag.of("a", "valueA"), Tag.of("b", "valueB"), Tag.of("c", "valueC")));
    }

    @Test
    void shouldNotExcludeNotExistingLabels() {
        cut = new ExcludeTagsFilter(CATEGORY, List.of("label3", "label4"));

        when(meterId.getName()).thenReturn(CATEGORY);
        when(meterId.getTagsAsIterable())
            .thenReturn(
                List.of(
                    Tag.of("a", "valueA"),
                    Tag.of("b", "valueB"),
                    Tag.of("label1", "value"),
                    Tag.of("label2", "value"),
                    Tag.of("c", "valueC")
                )
            );

        final Meter.Id mapped = cut.map(meterId);

        assertSame(meterId, mapped);
        verify(meterId, never()).replaceTags(any());
    }

    @Test
    void shouldNotExcludeLabelFromAnotherCategory() {
        cut = new ExcludeTagsFilter("other", List.of("label1", "label2"));

        when(meterId.getName()).thenReturn(CATEGORY);

        final Meter.Id mapped = cut.map(meterId);

        assertSame(meterId, mapped);
        verify(meterId, never()).replaceTags(any());
    }
}
