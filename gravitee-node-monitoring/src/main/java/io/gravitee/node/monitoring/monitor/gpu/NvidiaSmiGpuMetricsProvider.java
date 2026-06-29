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
package io.gravitee.node.monitoring.monitor.gpu;

import io.gravitee.node.api.monitor.GpuInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.CustomLog;

/**
 * {@link GpuMetricsProvider} backed by the NVIDIA {@code nvidia-smi} command line
 * tool. It shells out and parses the CSV output, mirroring the defensive,
 * dependency-free style used by {@code OsProbe} and {@code GcPressureProbe}.
 *
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class NvidiaSmiGpuMetricsProvider implements GpuMetricsProvider {

    static final String PROVIDER_NAME = "nvidia-smi";

    private static final long TIMEOUT_SECONDS = 5;

    /** The metrics queried, in order. Keep aligned with {@link #parse(String)}. */
    private static final String QUERY =
        "index,name,uuid,driver_version,utilization.gpu,utilization.memory,memory.total,memory.free,temperature.gpu,power.draw";

    private static final long MIB_TO_BYTES = 1024L * 1024L;

    /** The command used to invoke nvidia-smi. Overridable for testing. */
    private final String command;

    /** Memoized availability: GPU presence is static for the node's lifetime, so probe it only once. */
    private volatile Boolean available;

    public NvidiaSmiGpuMetricsProvider() {
        this(PROVIDER_NAME);
    }

    NvidiaSmiGpuMetricsProvider(String command) {
        this.command = command;
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        Boolean result = available;
        if (result == null) {
            synchronized (this) {
                result = available;
                if (result == null) {
                    result = probeAvailability();
                    available = result;
                }
            }
        }
        return result;
    }

    private boolean probeAvailability() {
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            // drain output to avoid blocking, then wait
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // discard
                }
            }
            return process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            log.debug("nvidia-smi is not available", e);
            return false;
        }
    }

    @Override
    public List<GpuInfo.Device> readDevices() {
        try {
            Process process = new ProcessBuilder(command, "--query-gpu=" + QUERY, "--format=csv,noheader,nounits")
                .redirectErrorStream(false)
                .start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                output = sb.toString();
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.debug("nvidia-smi timed out");
                return new ArrayList<>();
            }
            if (process.exitValue() != 0) {
                log.debug("nvidia-smi exited with code {}", process.exitValue());
                return new ArrayList<>();
            }
            return parse(output);
        } catch (Exception e) {
            log.debug("Unable to read GPU metrics from nvidia-smi", e);
            return new ArrayList<>();
        }
    }

    /**
     * Parses the raw {@code nvidia-smi --format=csv,noheader,nounits} output into a list of devices.
     * Fields reported as {@code [N/A]} (or unparseable) are left at their default value.
     */
    List<GpuInfo.Device> parse(String output) {
        List<GpuInfo.Device> devices = new ArrayList<>();
        if (output == null) {
            return devices;
        }
        for (String rawLine : output.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = line.split("\\s*,\\s*");
            if (fields.length < 10) {
                log.debug("Skipping unexpected nvidia-smi line: {}", line);
                continue;
            }
            GpuInfo.Mem mem = new GpuInfo.Mem(toBytes(parseLong(fields[6], -1)), toBytes(parseLong(fields[7], -1)));
            GpuInfo.Device device = new GpuInfo.Device(
                (int) parseLong(fields[0], -1),
                parseString(fields[1]),
                parseString(fields[2]),
                parseString(fields[3]),
                (short) parseLong(fields[4], -1),
                (short) parseLong(fields[5], -1),
                mem,
                (short) parseLong(fields[8], -1),
                parseDouble(fields[9])
            );
            devices.add(device);
        }
        return devices;
    }

    private static long toBytes(long mib) {
        return mib < 0 ? -1 : mib * MIB_TO_BYTES;
    }

    private static boolean isNotAvailable(String value) {
        return value == null || value.isEmpty() || value.equalsIgnoreCase("[N/A]") || value.equalsIgnoreCase("N/A");
    }

    private static String parseString(String value) {
        return isNotAvailable(value) ? null : value;
    }

    private static long parseLong(String value, long defaultValue) {
        if (isNotAvailable(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double parseDouble(String value) {
        if (isNotAvailable(value)) {
            return -1;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
