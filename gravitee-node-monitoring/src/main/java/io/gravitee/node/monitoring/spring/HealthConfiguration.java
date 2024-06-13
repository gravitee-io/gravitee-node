package io.gravitee.node.monitoring.spring;

import java.util.concurrent.TimeUnit;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public record HealthConfiguration(boolean enabled, long delay, TimeUnit unit, int cpuThreshold, int memoryThreshold) {}
