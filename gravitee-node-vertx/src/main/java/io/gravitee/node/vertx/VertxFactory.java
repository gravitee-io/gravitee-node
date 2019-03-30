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
import io.gravitee.node.vertx.verticle.factory.SpringVerticleFactory;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.*;
import io.vertx.micrometer.backends.BackendRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.EnumSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxFactory implements FactoryBean<Vertx> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxFactory.class);

    @Autowired
    private Node node;

    @Autowired
    private Environment environment;

    @Autowired
    private SpringVerticleFactory springVerticleFactory;

    @Override
    public Vertx getObject() throws Exception {
        LOGGER.debug("Creating a new instance of Vert.x");
        VertxOptions options = new VertxOptions();

        boolean metricsEnabled = environment.getProperty("services.metrics.enabled", Boolean.class, false);
        if (metricsEnabled) {
            configureMetrics(options);
        }

        Vertx instance = Vertx.vertx(options);
        instance.registerVerticleFactory(springVerticleFactory);

        if (metricsEnabled) {
            MeterRegistry registry = BackendRegistries.getDefaultNow();

            registry.config()
                    .meterFilter(new RenameVertxFilter())
                    .commonTags("application", node.application())
                    .commonTags("instance", node.hostname());

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
                .setDisabledMetricsCategories(EnumSet.of(
                        MetricsDomain.DATAGRAM_SOCKET,
                        MetricsDomain.NAMED_POOLS,
                        MetricsDomain.VERTICLES,
                        MetricsDomain.EVENT_BUS))
                .setEnabled(true);

        Match pathMatch = new Match()
                .setLabel("path")
                .setType(MatchType.REGEX)
                .setValue(".*")
                .setAlias("_");
        Match remoteMatch = new Match()
                .setLabel("remote")
                .setType(MatchType.REGEX)
                .setValue(".*")
                .setAlias("_");

        micrometerMetricsOptions
                .addLabelMatch(pathMatch)
                .addLabelMatch(remoteMatch);

        options.setMetricsOptions(micrometerMetricsOptions);

        boolean prometheusEnabled = environment.getProperty("services.metrics.prometheus.enabled", Boolean.class, true);
        if (prometheusEnabled) {
            LOGGER.info("Prometheus metrics support is enabled");
            micrometerMetricsOptions.setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true));
        }

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

    private class RenameVertxFilter implements MeterFilter {

        @Override
        public Meter.Id map(Meter.Id id) {
            if (id.getName().startsWith("vertx.")) {
                return id.withName(id.getName().substring(6));
            }

            return id;
        }
    }
}

