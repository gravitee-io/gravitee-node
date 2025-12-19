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

import io.gravitee.node.api.Node;
import io.gravitee.node.vertx.metrics.ExcludeTagsFilter;
import io.gravitee.node.vertx.metrics.RenameVertxFilter;
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.netty.buffer.ByteBufAllocatorMetricProvider;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;
import io.vertx.micrometer.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class VertxFactory implements FactoryBean<Vertx> {

    private static final EnumSet<Label> DEFAULT_LABELS = EnumSet.of(
        Label.LOCAL,
        Label.HTTP_METHOD,
        Label.HTTP_CODE,
        Label.POOL_NAME,
        Label.POOL_TYPE
    );

    private static final String PROMETHEUS_LABEL_VERSION_3_10 = "3.10";

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    @Autowired
    private SpringVerticleFactory springVerticleFactory;

    private Set<Label> metricsLabels;

    private Map<String, Set<Label>> metricsExcludedLabelsByCategory;

    @Override
    public Vertx getObject() throws Exception {
        log.debug("Creating a new instance of Vert.x");
        VertxOptions options = getVertxOptions();

        VertxBuilder vertxBuilder = Vertx.builder().with(options);

        boolean metricsEnabled = environment.getProperty("services.metrics.enabled", Boolean.class, false);
        CompositeMeterRegistry compositeMeterRegistry = null;

        if (metricsEnabled) {
            configureMetrics(options);

            String[] ignoreLabels = Arrays
                .stream(Label.values())
                .filter(label -> !metricsLabels.contains(label))
                .map(Label::toString)
                .toArray(String[]::new);

            //Global composite registry
            compositeMeterRegistry = new CompositeMeterRegistry();
            compositeMeterRegistry
                .config()
                .meterFilter(new RenameVertxFilter())
                .commonTags("application", node.application())
                .commonTags("instance", node.hostname())
                .meterFilter(MeterFilter.ignoreTags(ignoreLabels));

            final CompositeMeterRegistry finalCompositeMeterRegistry = compositeMeterRegistry;
            metricsExcludedLabelsByCategory.forEach((category, labels) ->
                finalCompositeMeterRegistry
                    .config()
                    .meterFilter(new ExcludeTagsFilter(category, labels.stream().map(String::valueOf).toList()))
            );

            boolean prometheusEnabled = environment.getProperty("services.metrics.prometheus.enabled", Boolean.class, true);
            if (prometheusEnabled) {
                log.info("Prometheus metrics support is enabled");
                var micrometerMetricsOptions = (MicrometerMetricsOptions) options.getMetricsOptions();
                micrometerMetricsOptions.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));

                //Prometheus registry
                MeterRegistry promRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

                //Add Prometheus registry to the composite registry
                compositeMeterRegistry.add(promRegistry);
            }

            vertxBuilder.withMetrics(new MicrometerMetricsFactory(compositeMeterRegistry));
        }
        Vertx instance = vertxBuilder.build();

        if (metricsEnabled) {
            //Load binders
            setBinders(compositeMeterRegistry, instance);
        }

        instance.registerVerticleFactory(springVerticleFactory);

        return instance;
    }

    private void configureMetrics(VertxOptions options) {
        log.info("Metrics support is enabled");

        //Read configured metrics categories
        Set<MetricsDomain> metricsDomains = loadMetricsDomains();

        Set<String> disabledMetricsDomains;
        // Default configuration
        if (metricsDomains.isEmpty()) {
            disabledMetricsDomains =
                new HashSet<>(
                    Arrays.asList(
                        MetricsDomain.DATAGRAM_SOCKET.toCategory(),
                        MetricsDomain.VERTICLES.toCategory(),
                        MetricsDomain.EVENT_BUS.toCategory()
                    )
                );
        } else {
            disabledMetricsDomains =
                Arrays
                    .stream(MetricsDomain.values())
                    .filter(metricsDomain -> !metricsDomains.contains(metricsDomain))
                    .map(MetricsDomain::toCategory)
                    .collect(Collectors.toSet());
        }

        MicrometerMetricsOptions micrometerMetricsOptions = new MicrometerMetricsOptions();
        micrometerMetricsOptions.setDisabledMetricsCategories(disabledMetricsDomains).setEnabled(true);

        String namesVersion = environment.getProperty("services.metrics.prometheus.naming.version");

        // Ensure compatibility with previous labels (Vertx 3.x)
        if (PROMETHEUS_LABEL_VERSION_3_10.equals(namesVersion)) {
            micrometerMetricsOptions.setMetricsNaming(MetricsNaming.v3Names());
        }

        // Read labels
        this.loadMetricLabels();
        micrometerMetricsOptions.setLabels(DEFAULT_LABELS);

        options.setMetricsOptions(micrometerMetricsOptions);
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
            metricsLabels = DEFAULT_LABELS;
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

    private Set<Binder> loadBinders() {
        final Set<Binder> binders = new HashSet<>();

        String value;
        int counter = 0;

        while ((value = environment.getProperty("services.metrics.binder[" + (counter++) + "]")) != null) {
            binders.add(Binder.valueOf(value));
        }

        //If empty, keep same binders to avoid breaking change
        if (binders.isEmpty()) {
            binders.addAll(
                List.of(
                    Binder.FILE_DESCRIPTOR,
                    Binder.CLASS_LOADER,
                    Binder.JVM_MEMORY,
                    Binder.JVM_GC_METRICS,
                    Binder.JVM_THREAD,
                    Binder.PROCESSOR
                )
            );
        }

        return binders;
    }

    private Set<MetricsDomain> loadMetricsDomains() {
        final Set<MetricsDomain> metricsDomains = new HashSet<>();

        String value;
        int counter = 0;
        while ((value = environment.getProperty("services.metrics.domain[" + (counter++) + "]")) != null) {
            metricsDomains.add(MetricsDomain.valueOf(value));
        }

        return metricsDomains;
    }

    private void setBinders(CompositeMeterRegistry compositeMeterRegistry, Vertx instance) {
        Set<Binder> binders = loadBinders();

        for (Binder binder : binders) {
            switch (binder) {
                case PROCESSOR -> new ProcessorMetrics().bindTo(compositeMeterRegistry);
                case CLASS_LOADER -> new ClassLoaderMetrics().bindTo(compositeMeterRegistry);
                case FILE_DESCRIPTOR -> new FileDescriptorMetrics().bindTo(compositeMeterRegistry);
                case JVM_GC_METRICS -> new JvmGcMetrics().bindTo(compositeMeterRegistry);
                case JVM_MEMORY -> new JvmMemoryMetrics().bindTo(compositeMeterRegistry);
                case JVM_THREAD -> new JvmThreadMetrics().bindTo(compositeMeterRegistry);
                case NETTY_ALLOCATOR -> {
                    new NettyAllocatorMetrics(UnpooledByteBufAllocator.DEFAULT).bindTo(compositeMeterRegistry);
                    new NettyAllocatorMetrics(PooledByteBufAllocator.DEFAULT).bindTo(compositeMeterRegistry);

                    new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.POOLED_ALLOCATOR)
                        .bindTo(compositeMeterRegistry);
                    new NettyAllocatorMetrics((ByteBufAllocatorMetricProvider) VertxByteBufAllocator.UNPOOLED_ALLOCATOR)
                        .bindTo(compositeMeterRegistry);
                }
                case NETTY_EVENT_EXECUTOR -> new NettyEventExecutorMetrics(instance.nettyEventLoopGroup()).bindTo(compositeMeterRegistry);
            }
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
        log.debug("Looking for metrics labels...");
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

        options.setPreferNativeTransport(environment.getProperty("vertx.preferNativeTransport", Boolean.class, true));

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

    public enum Binder {
        FILE_DESCRIPTOR,
        CLASS_LOADER,
        PROCESSOR,
        JVM_MEMORY,
        JVM_GC_METRICS,
        JVM_THREAD,
        NETTY_ALLOCATOR,
        NETTY_EVENT_EXECUTOR,
    }
}
