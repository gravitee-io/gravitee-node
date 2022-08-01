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
package io.gravitee.node.management.http.node.heap;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HotSpotHeapDumpSupplier implements HeapDumpSupplier {

    private Object diagnosticMXBean;

    private Method dumpHeapMethod;

    @SuppressWarnings("unchecked")
    HotSpotHeapDumpSupplier() {
        try {
            Class<?> diagnosticMXBeanClass = ClassUtils.resolveClassName("com.sun.management.HotSpotDiagnosticMXBean", null);
            this.diagnosticMXBean = ManagementFactory.getPlatformMXBean((Class<PlatformManagedObject>) diagnosticMXBeanClass);
            this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass, "dumpHeap", String.class, Boolean.TYPE);
        } catch (Throwable ex) {
            throw new HeapDumpException("Unable to locate HotSpotDiagnosticMXBean", ex);
        }
    }

    @Override
    public void dump(File file, boolean live) {
        ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean, file.getAbsolutePath(), live);
    }
}
