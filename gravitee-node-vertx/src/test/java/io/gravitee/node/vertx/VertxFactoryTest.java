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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.node.api.Node;
import io.gravitee.node.tracing.vertx.LazyVertxTracerFactory;
import io.gravitee.node.vertx.metrics.ExcludeTagsFilter;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.Label;
import io.vertx.micrometer.MetricsNaming;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class VertxFactoryTest {

    @Mock
    private Node node;

    @Spy
    private MockEnvironment environment = new MockEnvironment();

    @Mock
    private SpringVerticleFactory springVerticleFactory;

    @Mock
    private LazyVertxTracerFactory vertxTracerFactory;

    @Mock
    private Vertx vertx;

    @Mock
    private PrometheusMeterRegistry registry;

    private MockedStatic<Vertx> vertxStatic;

    private MockedStatic<BackendRegistries> registryStatic;

    @InjectMocks
    private VertxFactory cut;

    @BeforeEach
    void init() {
        vertxStatic = Mockito.mockStatic(Vertx.class);
        registryStatic = Mockito.mockStatic(BackendRegistries.class);

        vertxStatic.when(() -> Vertx.vertx(any(VertxOptions.class))).thenReturn(vertx);
        registry = spy(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
        registryStatic.when(BackendRegistries::getDefaultNow).thenReturn(registry);

        environment.setProperty("services.metrics.enabled", "false");
        environment.setProperty("services.tracing.enabled", "false");
    }

    @AfterEach
    void cleanup() {
        vertxStatic.close();
        registryStatic.close();
    }

    @Test
    void shouldEnableMetrics() throws Exception {
        enableMetrics();

        cut.getObject();

        verify(registry, atLeastOnce()).config();
    }

    @Test
    void shouldEnableMetricsWithVertxV3Names() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.prometheus.naming.version", "3.10");

        try (final MockedStatic<MetricsNaming> staticMetricsNaming = mockStatic(MetricsNaming.class)) {
            final MetricsNaming v3Naming = new MetricsNaming();
            staticMetricsNaming.when(MetricsNaming::v3Names).thenReturn(v3Naming);
            cut.getObject();

            vertxStatic.verify(() ->
                Vertx.vertx(
                    argThat(options -> {
                        final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                        return Objects.equals(v3Naming, metricsOptions.getMetricsNaming());
                    })
                )
            );
        }
    }

    @Test
    void shouldEnableMetricsWithLabels() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.labels[0]", "local");
        environment.setProperty("services.metrics.labels[1]", "remote");
        environment.setProperty("services.metrics.labels[2]", "http_method");
        environment.setProperty("services.metrics.labels[3]", "http_code");

        cut.getObject();

        vertxStatic.verify(() ->
            Vertx.vertx(
                argThat(options -> {
                    final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                    return metricsOptions.getLabels().containsAll(Set.of(Label.LOCAL, Label.REMOTE, Label.HTTP_METHOD, Label.HTTP_CODE));
                })
            )
        );

        verify(registry, atLeastOnce()).config();
    }

    @Test
    void shouldEnableMetricsWithLabelsFromInclude() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.http.client[0]", "remote");
        environment.setProperty("services.metrics.include.http.client[1]", "http_method");
        environment.setProperty("services.metrics.include.http.client[2]", "http_code");
        environment.setProperty("services.metrics.include.http.server[0]", "local");

        cut.getObject();

        vertxStatic.verify(() ->
            Vertx.vertx(
                argThat(options -> {
                    final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                    return metricsOptions.getLabels().containsAll(Set.of(Label.LOCAL, Label.REMOTE, Label.HTTP_METHOD, Label.HTTP_CODE));
                })
            )
        );

        verify(registry, atLeastOnce()).config();
    }

    @Test
    void shouldEnableMetricsWithExcludedLabels() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.labels[0]", "local");
        environment.setProperty("services.metrics.labels[1]", "remote");
        environment.setProperty("services.metrics.labels[2]", "http_method");
        environment.setProperty("services.metrics.labels[3]", "http_code");
        environment.setProperty("services.metrics.exclude.http.client[0]", "local");
        environment.setProperty("services.metrics.exclude.http.server[0]", "remote");

        final MeterRegistry.Config registryConfig = spy(registry.config());
        ReflectionTestUtils.setField(registry, "config", registryConfig);

        cut.getObject();

        vertxStatic.verify(() ->
            Vertx.vertx(
                argThat(options -> {
                    final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                    return metricsOptions.getLabels().containsAll(Set.of(Label.LOCAL, Label.REMOTE, Label.HTTP_METHOD, Label.HTTP_CODE));
                })
            )
        );

        verify(registry, atLeastOnce()).config();

        // Check that the appropriate meter tags filters have been passed to the registry config.
        verify(registryConfig)
            .meterFilter(
                argThat(filter ->
                    filter instanceof ExcludeTagsFilter &&
                    ((ExcludeTagsFilter) filter).getCategory().equals("http.client") &&
                    ((ExcludeTagsFilter) filter).getExcludedLabels().contains("local")
                )
            );
        verify(registryConfig)
            .meterFilter(
                argThat(filter ->
                    filter instanceof ExcludeTagsFilter &&
                    ((ExcludeTagsFilter) filter).getCategory().equals("http.server") &&
                    ((ExcludeTagsFilter) filter).getExcludedLabels().contains("remote")
                )
            );
    }

    @Test
    void shouldEnableMetricsWithExcludedLabelsForAllOtherCategoriesWhenLabelIsNotGloballyIncluded() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.http.client[0]", "remote");

        final MeterRegistry.Config registryConfig = spy(registry.config());
        ReflectionTestUtils.setField(registry, "config", registryConfig);

        cut.getObject();

        // Only 'local' tag should be added globally because of the include config.
        vertxStatic.verify(() ->
            Vertx.vertx(
                argThat(options -> {
                    final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                    return metricsOptions.getLabels().containsAll(Set.of(Label.LOCAL, Label.REMOTE, Label.HTTP_METHOD, Label.HTTP_CODE));
                })
            )
        );

        verify(registry, atLeastOnce()).config();

        // Check that 'remote' tags has been excluded for all categories except 'http.client'.
        verify(registryConfig, atLeastOnce())
            .meterFilter(
                argThat(filter ->
                    filter instanceof ExcludeTagsFilter &&
                    !((ExcludeTagsFilter) filter).getCategory().equals("http.client") &&
                    ((ExcludeTagsFilter) filter).getExcludedLabels().contains("remote")
                )
            );
    }

    @Test
    void shouldEnableMetricsWithoutLabelsFromUnknownCategory() throws Exception {
        enableMetrics();
        environment.setProperty("services.metrics.include.unknown[0]", "label1");
        environment.setProperty("services.metrics.include.http.client[0]", "remote");
        environment.setProperty("services.metrics.include.http.server[0]", "local");

        cut.getObject();

        vertxStatic.verify(() ->
            Vertx.vertx(
                argThat(options -> {
                    final MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                    return metricsOptions.getLabels().containsAll(Set.of(Label.LOCAL, Label.REMOTE));
                })
            )
        );

        verify(registry, atLeastOnce()).config();
    }

    @Test
    void shouldDisableMetrics() throws Exception {
        environment.setProperty("services.metrics.enabled", "false");

        cut.getObject();

        verify(registry, never()).config();
    }

    private void enableMetrics() {
        environment.setProperty("services.metrics.enabled", "true");
        when(node.application()).thenReturn("graviteeio-test");
        when(node.hostname()).thenReturn("localhost");
    }
}
