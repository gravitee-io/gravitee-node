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
package io.gravitee.node.monitoring.monitor.probe;

import io.gravitee.node.api.monitor.ProcessInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import lombok.CustomLog;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ProcessProbe {

    private static final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

    private static final Method getMaxFileDescriptorCountField;
    private static final Method getOpenFileDescriptorCountField;
    private static final Method getProcessCpuLoad;
    private static final Method getProcessCpuTime;
    private static final Method getCommittedVirtualMemorySize;

    static {
        getMaxFileDescriptorCountField = getUnixMethod("getMaxFileDescriptorCount");
        getOpenFileDescriptorCountField = getUnixMethod("getOpenFileDescriptorCount");
        getProcessCpuLoad = getMethod("getProcessCpuLoad");
        getProcessCpuTime = getMethod("getProcessCpuTime");
        getCommittedVirtualMemorySize = getMethod("getCommittedVirtualMemorySize");
    }

    private static class ProcessProbeHolder {

        private static final ProcessProbe INSTANCE = new ProcessProbe();
    }

    public static ProcessProbe getInstance() {
        return ProcessProbeHolder.INSTANCE;
    }

    private ProcessProbe() {}

    /**
     * Returns the maximum number of file descriptors allowed on the system, or -1 if not supported.
     */
    public long getMaxFileDescriptorCount() {
        if (getMaxFileDescriptorCountField == null) {
            return -1;
        }
        try {
            return (Long) getMaxFileDescriptorCountField.invoke(osMxBean);
        } catch (Exception ex) {
            log.debug("Unexpected exception", ex);
            return -1;
        }
    }

    /**
     * Returns the number of opened file descriptors associated with the current process, or -1 if not supported.
     */
    public long getOpenFileDescriptorCount() {
        if (getOpenFileDescriptorCountField == null) {
            return -1;
        }
        try {
            return (Long) getOpenFileDescriptorCountField.invoke(osMxBean);
        } catch (Exception ex) {
            log.debug("Unexpected exception", ex);
            return -1;
        }
    }

    public ProcessInfo processInfo() {
        ProcessInfo info = new ProcessInfo();

        info.timestamp = System.currentTimeMillis();
        info.openFileDescriptors = getOpenFileDescriptorCount();
        info.maxFileDescriptors = getMaxFileDescriptorCount();

        info.cpu = new ProcessInfo.Cpu();
        info.cpu.percent = getProcessCpuPercent();
        info.cpu.total = getProcessCpuTotalTime();

        info.mem = new ProcessInfo.Mem();
        info.mem.totalVirtual = getTotalVirtualMemorySize();

        return info;
    }

    /**
     * Returns the process CPU usage in percent
     */
    public short getProcessCpuPercent() {
        if (getProcessCpuLoad != null) {
            try {
                double load = (double) getProcessCpuLoad.invoke(osMxBean);
                if (load >= 0) {
                    return (short) (load * 100);
                }
            } catch (Throwable t) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns the CPU time (in milliseconds) used by the process on which the Java virtual machine is running, or -1 if not supported.
     */
    public long getProcessCpuTotalTime() {
        if (getProcessCpuTime != null) {
            try {
                long time = (long) getProcessCpuTime.invoke(osMxBean);
                if (time >= 0) {
                    return (time / 1_000_000L);
                }
            } catch (Exception t) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns the size (in bytes) of virtual memory that is guaranteed to be available to the running process
     */
    public long getTotalVirtualMemorySize() {
        if (getCommittedVirtualMemorySize != null) {
            try {
                long virtual = (long) getCommittedVirtualMemorySize.invoke(osMxBean);
                if (virtual >= 0) {
                    return virtual;
                }
            } catch (Exception t) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Returns a given method of the OperatingSystemMXBean,
     * or null if the method is not found or unavailable.
     */
    private static Method getMethod(String methodName) {
        try {
            return Class.forName("com.sun.management.OperatingSystemMXBean").getMethod(methodName);
        } catch (Throwable t) {
            // not available
            return null;
        }
    }

    /**
     * Returns a given method of the UnixOperatingSystemMXBean,
     * or null if the method is not found or unavailable.
     */
    private static Method getUnixMethod(String methodName) {
        try {
            return Class.forName("com.sun.management.UnixOperatingSystemMXBean").getMethod(methodName);
        } catch (Throwable t) {
            // not available
            return null;
        }
    }
}
