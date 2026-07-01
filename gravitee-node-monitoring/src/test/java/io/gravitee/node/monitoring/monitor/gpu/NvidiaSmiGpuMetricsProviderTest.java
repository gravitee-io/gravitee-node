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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import io.gravitee.node.api.monitor.GpuInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Rémi SULTAN (remi.sultan at graviteesource.com)
 * @author GraviteeSource Team
 */
class NvidiaSmiGpuMetricsProviderTest {

    private final NvidiaSmiGpuMetricsProvider provider = new NvidiaSmiGpuMetricsProvider();

    @Test
    void should_parse_a_single_device_line() {
        String output = "0, NVIDIA A100, GPU-abc, 535.104.05, 42, 17, 40960, 30960, 55, 250.43";

        List<GpuInfo.Device> devices = provider.parse(output);

        assertThat(devices).hasSize(1);
        GpuInfo.Device device = devices.get(0);
        assertThat(device.index()).isEqualTo(0);
        assertThat(device.name()).isEqualTo("NVIDIA A100");
        assertThat(device.uuid()).isEqualTo("GPU-abc");
        assertThat(device.driverVersion()).isEqualTo("535.104.05");
        assertThat(device.utilizationPercent()).isEqualTo((short) 42);
        assertThat(device.memoryUtilizationPercent()).isEqualTo((short) 17);
        assertThat(device.mem().total()).isEqualTo(40960L * 1024 * 1024);
        assertThat(device.mem().free()).isEqualTo(30960L * 1024 * 1024);
        assertThat(device.mem().getUsed()).isEqualTo((40960L - 30960L) * 1024 * 1024);
        assertThat(device.temperature()).isEqualTo((short) 55);
        assertThat(device.powerWatts()).isEqualTo(250.43);
    }

    @Test
    void should_parse_multiple_devices() {
        String output =
            "0, NVIDIA A100, GPU-abc, 535.104.05, 42, 17, 40960, 30960, 55, 250.43\n" +
            "1, NVIDIA A100, GPU-def, 535.104.05, 0, 0, 40960, 40000, 30, 60.00\n";

        List<GpuInfo.Device> devices = provider.parse(output);

        assertThat(devices).hasSize(2);
        assertThat(devices.get(1).index()).isEqualTo(1);
        assertThat(devices.get(1).uuid()).isEqualTo("GPU-def");
    }

    @Test
    void should_leave_defaults_for_not_available_fields() {
        String output = "0, NVIDIA A100, GPU-abc, 535.104.05, [N/A], [N/A], 40960, 30960, [N/A], [N/A]";

        List<GpuInfo.Device> devices = provider.parse(output);

        assertThat(devices).hasSize(1);
        GpuInfo.Device device = devices.get(0);
        assertThat(device.utilizationPercent()).isEqualTo((short) -1);
        assertThat(device.memoryUtilizationPercent()).isEqualTo((short) -1);
        assertThat(device.temperature()).isEqualTo((short) -1);
        assertThat(device.powerWatts()).isEqualTo(-1);
    }

    @Test
    void should_return_empty_list_for_blank_or_null_output() {
        assertThat(provider.parse(null)).isEmpty();
        assertThat(provider.parse("")).isEmpty();
        assertThat(provider.parse("\n  \n")).isEmpty();
    }

    @Test
    void should_skip_malformed_lines() {
        String output = "garbage,too,few,fields";

        assertThat(provider.parse(output)).isEmpty();
    }

    @Test
    @DisabledOnOs(WINDOWS)
    void should_read_devices_by_emulating_nvidia_smi(@TempDir Path tempDir) throws Exception {
        String fakeOutput =
            "0, NVIDIA A100, GPU-abc, 535.104.05, 42, 17, 40960, 30960, 55, 250.43\\n" +
            "1, NVIDIA A100, GPU-def, 535.104.05, 0, 0, 40960, 40000, 30, 60.00";
        Path fakeNvidiaSmi = writeFakeNvidiaSmi(tempDir, fakeOutput);

        NvidiaSmiGpuMetricsProvider emulated = new NvidiaSmiGpuMetricsProvider(fakeNvidiaSmi.toString());

        assertThat(emulated.isAvailable()).isTrue();

        List<GpuInfo.Device> devices = emulated.readDevices();
        assertThat(devices).hasSize(2);
        assertThat(devices.get(0).index()).isZero();
        assertThat(devices.get(0).name()).isEqualTo("NVIDIA A100");
        assertThat(devices.get(0).utilizationPercent()).isEqualTo((short) 42);
        assertThat(devices.get(0).mem().total()).isEqualTo(40960L * 1024 * 1024);
        assertThat(devices.get(0).powerWatts()).isEqualTo(250.43);
        assertThat(devices.get(1).uuid()).isEqualTo("GPU-def");
    }

    @Test
    @DisabledOnOs(WINDOWS)
    void should_return_empty_devices_when_emulated_nvidia_smi_fails(@TempDir Path tempDir) throws Exception {
        Path failing = tempDir.resolve("nvidia-smi");
        Files.writeString(failing, "#!/bin/sh\nexit 1\n");
        makeExecutable(failing);

        NvidiaSmiGpuMetricsProvider emulated = new NvidiaSmiGpuMetricsProvider(failing.toString());

        assertThat(emulated.isAvailable()).isFalse();
        assertThat(emulated.readDevices()).isEmpty();
    }

    @Test
    void should_report_unavailable_when_command_is_missing() {
        NvidiaSmiGpuMetricsProvider missing = new NvidiaSmiGpuMetricsProvider("/path/does/not/exist/nvidia-smi");

        assertThat(missing.isAvailable()).isFalse();
        assertThat(missing.readDevices()).isEmpty();
    }

    @Test
    @DisabledOnOs(WINDOWS)
    void should_probe_availability_only_once(@TempDir Path tempDir) throws Exception {
        Path versionCalls = tempDir.resolve("version-calls.log");
        Path nvidiaSmi = tempDir.resolve("nvidia-smi");
        // Record each --version invocation so we can count how many times availability was probed.
        Files.writeString(
            nvidiaSmi,
            "#!/bin/sh\n" + "if [ \"$1\" = \"--version\" ]; then echo x >> '" + versionCalls + "'; exit 0; fi\n" + "exit 1\n"
        );
        makeExecutable(nvidiaSmi);

        NvidiaSmiGpuMetricsProvider cut = new NvidiaSmiGpuMetricsProvider(nvidiaSmi.toString());

        assertThat(cut.isAvailable()).isTrue();
        assertThat(cut.isAvailable()).isTrue();
        assertThat(cut.isAvailable()).isTrue();

        assertThat(Files.readAllLines(versionCalls)).hasSize(1);
    }

    private static Path writeFakeNvidiaSmi(Path tempDir, String csvOutput) throws Exception {
        Path script = tempDir.resolve("nvidia-smi");
        // Emulate nvidia-smi: handle --version and --query-gpu invocations.
        String content =
            "#!/bin/sh\n" +
            "case \"$1\" in\n" +
            "  --version) echo 'NVIDIA-SMI 535.104.05'; exit 0;;\n" +
            "  --query-gpu=*) printf '" +
            csvOutput +
            "\\n'; exit 0;;\n" +
            "esac\n" +
            "exit 1\n";
        Files.writeString(script, content);
        makeExecutable(script);
        return script;
    }

    private static void makeExecutable(Path path) throws Exception {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }
}
