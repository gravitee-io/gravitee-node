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
package io.gravitee.node.vertx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.vertx.metrics.ExcludeTagsFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.netty.channel.EventLoopGroup;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.internal.VertxInternal;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.MicrometerMetricsFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxFactoryTest {

    @Mock
    private Node node;

    @Spy
    private MockEnvironment environment = new MockEnvironment();

    @Mock
    private VertxInternal vertx;

    @Mock
    private VertxBuilder vertxBuilder;

    private MockedStatic<Vertx> vertxStatic;

    @InjectMocks
    private VertxFactory cut;

    @BeforeEach
    void init() {
        vertxStatic = Mockito.mockStatic(Vertx.class);

        when(vertxBuilder.with(any())).thenReturn(vertxBuilder);
        when(vertxBuilder.build()).thenReturn(vertx);
        vertxStatic.when(Vertx::builder).thenReturn(vertxBuilder);

        EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        lenient().when(((VertxInternal) vertx).nettyEventLoopGroup()).thenReturn(eventLoopGroup);

        environment.setProperty("services.metrics.enabled", "false");
        environment.setProperty("services.opentelemetry.enabled", "false");
    }

    @AfterEach
    void cleanup() {
        vertxStatic.close();
    }

    @Test
    void should_enable_metrics() throws Exception {
        enableMetrics();

        cut.getObject();

        verify(vertxBuilder).withMetrics(argThat(metricsFactory -> metricsFactory instanceof MicrometerMetricsFactory));
    }

    @Test
    void should_enable_metrics_with_vertx_v3_names() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.prometheus.naming.version", "3.10");

        try (final MockedStatic<MetricsNaming> staticMetricsNaming = mockStatic(MetricsNaming.class)) {
            final MetricsNaming v3Naming = new MetricsNaming();
            staticMetricsNaming.when(MetricsNaming::v3Names).thenReturn(v3Naming);
            cut.getObject();

            verify(vertxBuilder)
                .with(
                    argThat(options -> {
                        final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                        return Objects.equals(v3Naming, metricsOptions.getMetricsNaming());
                    })
                );
        }
    }

    @Test
    void should_enable_metrics_with_labels() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.labels[0]", "local");
        environment.setProperty("services.metrics.labels[1]", "remote");
        environment.setProperty("services.metrics.labels[2]", "http_method");
        environment.setProperty("services.metrics.labels[3]", "http_code");

        cut.getObject();

        verifyGlobalLabels(List.of(Label.LOCAL, Label.REMOTE, Label.HTTP_METHOD, Label.HTTP_CODE));
    }

    @Test
    void should_enable_metrics_with_labels_from_include() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.http.client[0]", "remote");
        environment.setProperty("services.metrics.include.http.client[1]", "http_method");
        environment.setProperty("services.metrics.include.http.client[2]", "http_code");
        environment.setProperty("services.metrics.include.http.server[0]", "local");

        cut.getObject();

        verifyGlobalLabels(List.of(Label.LOCAL, Label.HTTP_METHOD, Label.HTTP_CODE, Label.POOL_NAME, Label.POOL_TYPE, Label.REMOTE));
    }

    @Test
    void should_enable_metrics_with_excluded_labels() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.labels[0]", "local");
        environment.setProperty("services.metrics.labels[1]", "remote");
        environment.setProperty("services.metrics.exclude.http.client[0]", "local");
        environment.setProperty("services.metrics.exclude.http.server[0]", "remote");

        cut.getObject();

        final List<MeterFilter> filters = extractRegistryMeterFilters();
        verifyGlobalLabels(List.of(Label.LOCAL, Label.REMOTE));

        //Check exclude labels
        var httpClientFilter = new ExcludeTagsFilter("http.client", List.of("local"));
        var httpServerFilter = new ExcludeTagsFilter("http.server", List.of("remote"));
        assertThat(filters).filteredOn(f -> f instanceof ExcludeTagsFilter).containsAll(List.of(httpClientFilter, httpServerFilter));

        //Check allowed labels
        assertThat(filters)
            .filteredOn(f -> f instanceof ExcludeTagsFilter)
            .filteredOn(f ->
                !((ExcludeTagsFilter) f).category().contains("http.server") && !((ExcludeTagsFilter) f).category().contains("http.client")
            )
            .allSatisfy(f -> assertThat(((ExcludeTagsFilter) f).excludedLabels()).isEmpty());
    }

    @Test
    void should_enable_metrics_with_excluded_labels_for_all_other_categories_when_label_is_not_globally_included() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.http.client[0]", "remote");

        cut.getObject();

        //Check remote label excluded for all categories except http.client
        final List<MeterFilter> filters = extractRegistryMeterFilters();
        verifyGlobalLabels(List.of(Label.LOCAL, Label.HTTP_METHOD, Label.HTTP_CODE, Label.POOL_NAME, Label.POOL_TYPE, Label.REMOTE));

        var httpClientFilter = new ExcludeTagsFilter("http.client", List.of());
        assertThat(filters).filteredOn(f -> f instanceof ExcludeTagsFilter).containsAll(List.of(httpClientFilter));

        assertThat(filters)
            .filteredOn(f -> f instanceof ExcludeTagsFilter)
            .filteredOn(f -> !((ExcludeTagsFilter) f).category().contains("http.client"))
            .allSatisfy(f -> assertThat(((ExcludeTagsFilter) f).excludedLabels()).isEqualTo(List.of(Label.REMOTE.toString())));
    }

    @Test
    void should_enable_metrics_without_labels_from_unknown_category() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.unknown[0]", "label1");
        environment.setProperty("services.metrics.include.http.client[0]", "remote");
        environment.setProperty("services.metrics.include.http.server[0]", "local");

        cut.getObject();

        verifyGlobalLabels(List.of(Label.LOCAL, Label.HTTP_METHOD, Label.HTTP_CODE, Label.POOL_NAME, Label.POOL_TYPE, Label.REMOTE));
    }

    @Test
    void should_disable_metrics() throws Exception {
        environment.setProperty("services.metrics.enabled", "false");

        cut.getObject();

        verify(vertxBuilder, never()).withMetrics(any());
    }

    @Test
    void should_enable_default_binders() throws Exception {
        enableMetrics();

        cut.getObject();

        verify(vertxBuilder)
            .withMetrics(
                argThat(metricsFactory -> {
                    MeterRegistry meterRegistry = (MeterRegistry) ReflectionTestUtils.getField(metricsFactory, "micrometerRegistry");
                    assertThat(meterRegistry).isNotNull();
                    Set<String> meterNames = meterRegistry
                        .getMeters()
                        .stream()
                        .map(meter -> meter.getId().getName())
                        .collect(Collectors.toSet());

                    assertThat(meterNames)
                        .containsExactly(
                            "system.load.average.1m",
                            "jvm.gc.max.data.size",
                            "system.cpu.usage",
                            "jvm.memory.committed",
                            "jvm.threads.peak",
                            "process.cpu.usage",
                            "jvm.threads.live",
                            "jvm.gc.live.data.size",
                            "process.files.max",
                            "jvm.threads.started",
                            "jvm.memory.max",
                            "jvm.gc.memory.promoted",
                            "jvm.memory.used",
                            "system.cpu.count",
                            "process.files.open",
                            "jvm.gc.memory.allocated",
                            "jvm.classes.loaded",
                            "jvm.classes.unloaded",
                            "jvm.buffer.memory.used",
                            "jvm.buffer.count",
                            "jvm.threads.daemon",
                            "jvm.threads.states",
                            "jvm.buffer.total.capacity",
                            "process.cpu.time"
                        );
                    return true;
                })
            );
    }

    @Test
    void should_enable_only_specific_binders() throws Exception {
        enableMetrics();

        environment.setProperty("services.metrics.binder[0]", "FILE_DESCRIPTOR");

        cut.getObject();

        verify(vertxBuilder)
            .withMetrics(
                argThat(metricsFactory -> {
                    MeterRegistry meterRegistry = (MeterRegistry) ReflectionTestUtils.getField(metricsFactory, "micrometerRegistry");
                    assertThat(meterRegistry).isNotNull();
                    Set<String> meterNames = meterRegistry
                        .getMeters()
                        .stream()
                        .map(meter -> meter.getId().getName())
                        .collect(Collectors.toSet());

                    assertThat(meterNames).containsExactly("process.files.open", "process.files.max");
                    return true;
                })
            );
    }

    @Test
    void should_enable_native_transport_by_default() throws Exception {
        cut.getObject();

        verify(vertxBuilder).with(argThat(VertxOptions::getPreferNativeTransport));
    }

    @Test
    void should_disable_native_transport() throws Exception {
        environment.setProperty("vertx.preferNativeTransport", "false");

        cut.getObject();

        verify(vertxBuilder).with(argThat(vertxOptions -> !vertxOptions.getPreferNativeTransport()));
    }

    private void enableMetrics() {
        environment.setProperty("services.metrics.enabled", "true");
        when(node.application()).thenReturn("graviteeio-test");
        when(node.hostname()).thenReturn("localhost");
    }

    private List<MeterFilter> extractRegistryMeterFilters() {
        return Arrays.asList((MeterFilter[]) ReflectionTestUtils.getField(extractMeterRegistry(), "filters"));
    }

    private void verifyGlobalLabels(List<Label> expectedLabels) {
        //Create a tag list with all labels
        List<Tag> tagList = Arrays.stream(Label.values()).map(Label::toString).map(name -> Tag.of(name, name)).toList();

        List<Tag> expectedTags = expectedLabels
            .stream()
            .map(Label::toString)
            .map(name -> Tag.of(name, name))
            .collect(Collectors.toCollection(ArrayList::new));
        expectedTags.add(Tag.of("application", "graviteeio-test"));
        expectedTags.add(Tag.of("instance", "localhost"));

        MeterRegistry meterRegistry = extractMeterRegistry();
        meterRegistry.counter("test", Tags.of(tagList)).increment();

        // Verify that the global meter filter only kept the configured labels
        meterRegistry
            .getMeters()
            .stream()
            .filter(meter -> meter.getId().getName().equals("test"))
            .forEach(m -> assertThat(m.getId().getTags()).containsExactlyInAnyOrderElementsOf(expectedTags));
    }

    private MeterRegistry extractMeterRegistry() {
        final ArgumentCaptor<MicrometerMetricsFactory> captor = ArgumentCaptor.forClass(MicrometerMetricsFactory.class);
        verify(vertxBuilder).withMetrics(captor.capture());
        final MicrometerMetricsFactory metricsFactory = captor.getValue();
        MeterRegistry meterRegistry = (MeterRegistry) ReflectionTestUtils.getField(metricsFactory, "micrometerRegistry");
        assertThat(meterRegistry).isNotNull();
        assertThat(meterRegistry).isInstanceOf(CompositeMeterRegistry.class);
        return meterRegistry;
    }
}
