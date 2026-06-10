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
package io.gravitee.node.reporter.vertx.eventbus;

import static org.mockito.Mockito.*;

import io.gravitee.reporter.api.ReportTarget;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.vertx.core.eventbus.Message;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventBusReporterWrapperTest {

    @Mock
    private Reporter reporter;

    @Mock
    private Message<Reportable> message;

    @Mock
    private Reportable reportable;

    private EventBusReporterWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new EventBusReporterWrapper(null, reporter);
    }

    @Nested
    class TargetFiltering {

        @Test
        void should_dispatch_when_targets_match() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(EnumSet.of(ReportTarget.TRACING));
            when(reporter.supportedTargets()).thenReturn(EnumSet.of(ReportTarget.TRACING));

            wrapper.handle(message);

            verify(reporter).report(reportable);
        }

        @Test
        void should_not_dispatch_when_targets_are_disjoint() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(EnumSet.of(ReportTarget.TRACING));
            when(reporter.supportedTargets()).thenReturn(EnumSet.of(ReportTarget.ANALYTICS));

            wrapper.handle(message);

            verify(reporter, never()).report(reportable);
        }

        @Test
        void should_dispatch_when_reportable_targets_both_and_reporter_supports_analytics() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(EnumSet.of(ReportTarget.ANALYTICS, ReportTarget.TRACING));
            when(reporter.supportedTargets()).thenReturn(EnumSet.of(ReportTarget.ANALYTICS));

            wrapper.handle(message);

            verify(reporter).report(reportable);
        }

        @Test
        void should_dispatch_when_both_use_defaults() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(ReportTarget.DEFAULT);
            when(reporter.supportedTargets()).thenReturn(ReportTarget.DEFAULT);

            wrapper.handle(message);

            verify(reporter).report(reportable);
        }

        @Test
        void should_not_dispatch_tracing_log_to_default_analytics_reporter() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(EnumSet.of(ReportTarget.TRACING));
            when(reporter.supportedTargets()).thenReturn(ReportTarget.DEFAULT);

            wrapper.handle(message);

            verify(reporter, never()).report(reportable);
        }

        @Test
        void should_not_dispatch_when_can_handle_is_false() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(false);

            wrapper.handle(message);

            verify(reporter, never()).report(reportable);
        }

        @Test
        void should_dispatch_when_reportable_targets_is_null() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(null);
            when(reporter.supportedTargets()).thenReturn(EnumSet.of(ReportTarget.ANALYTICS));

            wrapper.handle(message);

            verify(reporter).report(reportable);
        }

        @Test
        void should_dispatch_when_reporter_supported_targets_is_null() {
            when(message.body()).thenReturn(reportable);
            when(reporter.canHandle(reportable)).thenReturn(true);
            when(reportable.getTargets()).thenReturn(EnumSet.of(ReportTarget.TRACING));
            when(reporter.supportedTargets()).thenReturn(null);

            wrapper.handle(message);

            verify(reporter).report(reportable);
        }
    }
}
