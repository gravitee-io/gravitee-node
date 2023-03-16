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

import static java.util.stream.Collectors.toList;

import io.gravitee.node.api.Node;
import io.gravitee.node.tracing.vertx.LazyVertxTracerFactory;
import io.gravitee.node.vertx.metrics.ExcludeTagsFilter;
import io.gravitee.node.vertx.metrics.RenameVertxFilter;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.micrometer.*;
import io.vertx.micrometer.backends.BackendRegistries;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxFactory implements FactoryBean<Vertx> {

    private static final String PROMETHEUS_LABEL_VERSION_3_10 = "3.10";

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxFactory.class);

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    @Autowired
    private SpringVerticleFactory springVerticleFactory;

    @Autowired
    private LazyVertxTracerFactory vertxTracerFactory;

    private Set<Label> metricsLabels;

    private Map<String, Set<Label>> metricsExcludedLabelsByCategory;

    @Override
    public Vertx getObject() throws Exception {
        LOGGER.debug("Creating a new instance of Vert.x");
        VertxOptions options = getVertxOptions();

        boolean metricsEnabled = environment.getProperty("services.metrics.enabled", Boolean.class, false);
        if (metricsEnabled) {
            configureMetrics(options);
        }

        boolean tracingEnabled = environment.getProperty("services.tracing.enabled", Boolean.class, false);
        if (tracingEnabled) {
            configureTracing(options);
        }

        Vertx instance = Vertx.vertx(options);
        instance.registerVerticleFactory(springVerticleFactory);

        if (metricsEnabled) {
            MeterRegistry registry = BackendRegistries.getDefaultNow();

            registry
                .config()
                .meterFilter(new RenameVertxFilter())
                .commonTags("application", node.application())
                .commonTags("instance", node.hostname());

            metricsExcludedLabelsByCategory.forEach((category, labels) ->
                registry.config().meterFilter(new ExcludeTagsFilter(category, labels.stream().map(String::valueOf).collect(toList())))
            );

            new FileDescriptorMetrics().bindTo(registry);
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
        }

        return instance;
    }

    private void configureMetrics(VertxOptions options) {
        LOGGER.info("Metrics support is enabled");

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions();
        micrometerMetricsOptions
            .setDisabledMetricsCategories(
                new HashSet<>(
                    Arrays.asList(
                        MetricsDomain.DATAGRAM_SOCKET.toCategory(),
                        MetricsDomain.NAMED_POOLS.toCategory(),
                        MetricsDomain.VERTICLES.toCategory(),
                        MetricsDomain.EVENT_BUS.toCategory()
                    )
                )
            )
            .setEnabled(true);

        String namesVersion = environment.getProperty("services.metrics.prometheus.naming.version");

        // Ensure compatibility with previous labels (Vertx 3.x)
        if (PROMETHEUS_LABEL_VERSION_3_10.equals(namesVersion)) {
            micrometerMetricsOptions.setMetricsNaming(MetricsNaming.v3Names());
        }

        // Read labels
        this.loadMetricLabels();
        micrometerMetricsOptions.setLabels(metricsLabels);

        boolean prometheusEnabled = environment.getProperty("services.metrics.prometheus.enabled", Boolean.class, true);
        if (prometheusEnabled) {
            LOGGER.info("Prometheus metrics support is enabled");
            micrometerMetricsOptions.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
        }

        options.setMetricsOptions(micrometerMetricsOptions);
    }

    private void configureTracing(VertxOptions options) {
        options.setTracingOptions(new TracingOptions().setFactory(vertxTracerFactory));
    }

    @Override
    public Class<?> getObjectType() {
        return Vertx.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private void loadMetricLabels() {
        final Map<String, Set<Label>> includedLabelsByCategory = readConfiguredLabelsByCategory("include");

        // Include labels takes precedence over defined labels.
        Set<String> labels = readConfiguredLabels();
        if (labels != null && !labels.isEmpty()) {
            metricsLabels = labels.stream().map(this::toLabel).collect(Collectors.toSet());
        } else {
            metricsLabels = EnumSet.of(Label.LOCAL, Label.HTTP_METHOD, Label.HTTP_CODE);
        }

        // If a label is activated for a specific category, it must be added globally and then manually excluded for all other categories :-(
        includedLabelsByCategory.forEach((category, includedLabels) -> metricsLabels.addAll(includedLabels));

        metricsExcludedLabelsByCategory = readConfiguredLabelsByCategory("exclude");

        // Identify the labels to exclude for each category.
        for (Map.Entry<String, Set<Label>> labelsByCategory : includedLabelsByCategory.entrySet()) {
            final String includedCategory = labelsByCategory.getKey();
            final Set<Label> includedCategoryLabels = labelsByCategory.getValue();

            // Get the domains where these labels are not included (ie: domain on which to explicitly exclude this label).
            Arrays
                .stream(MetricsDomain.values())
                .map(MetricsDomain::toCategory)
                .filter(otherCategory -> !otherCategory.equalsIgnoreCase(includedCategory))
                .forEach(otherCategory -> {
                    if (includedLabelsByCategory.containsKey(otherCategory)) {
                        final Set<Label> otherCategoryLabels = includedLabelsByCategory.get(otherCategory);
                        includedCategoryLabels.forEach(label -> {
                            if (!otherCategoryLabels.contains(label)) {
                                // Label not explicitly included for this category, add it to the exclusion list.
                                metricsExcludedLabelsByCategory.computeIfAbsent(otherCategory, key -> new HashSet<>()).add(label);
                            }
                        });
                    } else {
                        // Directly exclude all the labels on the current category.
                        includedCategoryLabels.forEach(label ->
                            metricsExcludedLabelsByCategory.computeIfAbsent(otherCategory, key -> new HashSet<>()).add(label)
                        );
                    }
                });
        }
    }

    private Map<String, Set<Label>> readConfiguredLabelsByCategory(final String type) {
        final Map<String, Set<Label>> labelsByCategory = new HashMap<>();

        for (MetricsDomain metricsDomain : MetricsDomain.values()) {
            final String category = metricsDomain.toCategory();
            final Set<Label> labels = new HashSet<>();

            String value;
            int counter = 0;
            labelsByCategory.put(category, labels);

            while ((value = environment.getProperty("services.metrics." + type + "." + category + "[" + (counter++) + "]")) != null) {
                labels.add(toLabel(value));
            }
        }

        return labelsByCategory;
    }

    private Label toLabel(String label) {
        return Label.valueOf(label.toUpperCase());
    }

    private Set<String> readConfiguredLabels() {
        LOGGER.debug("Looking for metrics labels...");
        Set<String> labels = null;

        boolean found = true;
        int idx = 0;

        while (found) {
            String label = environment.getProperty("services.metrics.labels[" + idx + "]");
            found = (label != null);
            if (found) {
                if (labels == null) {
                    labels = new HashSet<>();
                }
                labels.add(label);
            }
            idx++;
        }

        return labels;
    }

    private VertxOptions getVertxOptions() {
        VertxOptions options = new VertxOptions();

        options.setPreferNativeTransport(true);

        Long blockedThreadCheckInterval = Long.getLong("vertx.options.blockedThreadCheckInterval");
        if (blockedThreadCheckInterval != null) {
            options.setBlockedThreadCheckInterval(blockedThreadCheckInterval);
        }

        Long maxEventLoopExecuteTime = Long.getLong("vertx.options.maxEventLoopExecuteTime");
        if (maxEventLoopExecuteTime != null) {
            options.setMaxEventLoopExecuteTime(maxEventLoopExecuteTime);
        }

        Long warningExceptionTime = Long.getLong("vertx.options.warningExceptionTime");
        if (warningExceptionTime != null) {
            options.setWarningExceptionTime(warningExceptionTime);
        }

        return options;
    }
}
