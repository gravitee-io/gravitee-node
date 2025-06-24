package io.gravitee.node.monitoring.healthcheck.probe;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import io.gravitee.node.api.monitor.JvmInfo;
import io.gravitee.node.monitoring.monitor.probe.JvmProbe;
import io.gravitee.node.monitoring.spring.HealthConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class GcPressureProbe implements Probe {

    @Autowired
    private HealthConfiguration healthConfiguration;

    private long lastTimestampInNanoseconds = 0;
    private long lastTotalGCCollectionTime = 0;
    private final double nbAvailableProcessors;

    public GcPressureProbe() {
        this.nbAvailableProcessors = this.getAvailableProcessors();
    }

    public GcPressureProbe(HealthConfiguration healthConfiguration) {
        this.healthConfiguration = healthConfiguration;
        this.nbAvailableProcessors = this.getAvailableProcessors();
    }

    @Override
    public String id() {
        return "gc-pressure";
    }

    @Override
    public boolean isCacheable() {
        return false;
    }

    @Override
    public boolean isVisibleByDefault() {
        return false;
    }

    @Override
    public CompletableFuture<Result> check() {
        try {
            return CompletableFuture.supplyAsync(() ->
                getGcCpuUsage() < healthConfiguration.gcPressureThreshold()
                    ? Result.healthy()
                    : Result.unhealthy(
                        String.format("GC Pressure is over the threshold of %d %%", healthConfiguration.gcPressureThreshold())
                    )
            );
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(Result.unhealthy(ex));
        }
    }

    /**
     * Compute a GC Cpu usage metric based on elapsed time, gcCollectionTime and availableProcessors
     * @return a percentage of cpu usage used by the GC
     */
    private double getGcCpuUsage() {
        long currentTimeInNanoseconds = System.nanoTime();

        // Collect all GC collection time and compute elapsedGcCollectionTime
        long gcCollectionTime = 0;
        for (JvmInfo.GarbageCollector collector : JvmProbe.getInstance().jvmInfo().gc.collectors) {
            gcCollectionTime += collector.getCollectionTime();
        }
        long elapsedGcCollectionTime = gcCollectionTime - lastTotalGCCollectionTime;
        lastTotalGCCollectionTime = gcCollectionTime;

        //Convert gcCollectionTime in nanoseconds
        long gcCollectionTimeInNanoseconds = TimeUnit.NANOSECONDS.convert(elapsedGcCollectionTime, TimeUnit.MILLISECONDS);

        // Ignore first call
        if (lastTimestampInNanoseconds == 0) {
            lastTimestampInNanoseconds = currentTimeInNanoseconds;
            return 0.0;
        }
        // elapsed time depends on the numbers of processor available
        double elapsedTime = (currentTimeInNanoseconds - lastTimestampInNanoseconds) * this.nbAvailableProcessors;
        lastTimestampInNanoseconds = currentTimeInNanoseconds;

        return (gcCollectionTimeInNanoseconds / elapsedTime) * 100;
    }

    /**
     * Method that checks if we are in a containerized environment
     * If so base the nb of processors on cgroups value otherwise return the value of Runtime.getRuntime().availableProcessors()
     * We need to use cgroups in containerized env since the JVM will always return at least 1
     * and not a more fine value like 0,5 (values lower than 1 are rounded up using ceil)
     * (see code here https://github.com/openjdk/jdk/blob/c2d76f9844aadf77a0b213a9169a7c5c8c8f1ffb/src/hotspot/os/linux/cgroupUtil_linux.cpp#L37)
     * @return the number of available processors
     */
    private double getAvailableProcessors() {
        try {
            if (isCgroupV2()) {
                return getCpuLimitCgroupV2();
            }
            return getCpuLimitCgroupV1();
        } catch (Exception e) {
            // Fallback to visible CPUs
            return Runtime.getRuntime().availableProcessors();
        }
    }

    private boolean isCgroupV2() {
        Path cgroupPath = Paths.get("/sys/fs/cgroup");
        return Files.exists(cgroupPath.resolve("cgroup.controllers")); // Only exists in cgroup v2
    }

    private double getCpuLimitCgroupV1() throws IOException {
        Path quotaPath = Paths.get("/sys/fs/cgroup/cpu/cpu.cfs_quota_us");
        Path periodPath = Paths.get("/sys/fs/cgroup/cpu/cpu.cfs_period_us");

        if (Files.exists(quotaPath) && Files.exists(periodPath)) {
            long quota = Long.parseLong(Files.readString(quotaPath).trim());
            long period = Long.parseLong(Files.readString(periodPath).trim());

            if (quota > 0 && period > 0) {
                return (double) quota / period;
            }
        }

        // If quota or period not found, default to availableProcessors()
        return Runtime.getRuntime().availableProcessors();
    }

    private double getCpuLimitCgroupV2() throws IOException, NumberFormatException {
        Path cpuMaxPath = Paths.get("/sys/fs/cgroup/cpu.max");
        if (Files.exists(cpuMaxPath)) {
            String content = Files.readString(cpuMaxPath).trim();
            String[] parts = content.split(" ");
            if (parts.length == 2 && "max".equals(parts[0])) {
                long quota = Long.parseLong(parts[0]);
                long period = Long.parseLong(parts[1]);

                return (double) quota / period;
            }
        }

        // Default to availableProcessors() if sthg missing
        return Runtime.getRuntime().availableProcessors();
    }
}
