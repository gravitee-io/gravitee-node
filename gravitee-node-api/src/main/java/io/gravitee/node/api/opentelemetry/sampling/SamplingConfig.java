/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.api.opentelemetry.sampling;

import java.util.List;
import java.util.Set;

/**
 * Which requests a tracer records, in two parts.
 * <p>
 * Head part, decided once per trace when its root span starts, from what is known at that moment: ratio, HTTP
 * methods, path prefixes and whether an incoming {@code traceparent} decides for us.
 * <p>
 * Tail part, known only at the end of the request: status codes and errors. The gateway cannot drop spans already
 * exported, so it only marks the trace; a tail-sampling collector keeps or drops it whole.
 *
 * @param ratio share of matching root spans recorded, between 0 and 1
 * @param respectRemoteParent whether an incoming {@code traceparent} decides for us; when false, the caller can no
 *                            longer turn tracing on or off for its own requests
 * @param methods HTTP methods recorded; empty means every method
 * @param pathPrefixes request path prefixes recorded; empty means every path
 * @param keepStatuses response statuses whose traces the collector keeps
 * @param keepErrors whether the collector keeps the traces of failed requests (5xx or error)
 */
public record SamplingConfig(
    double ratio,
    boolean respectRemoteParent,
    Set<String> methods,
    List<String> pathPrefixes,
    Set<Integer> keepStatuses,
    boolean keepErrors
) {
    public static final SamplingConfig ALWAYS = new SamplingConfig(1.0, true, Set.of(), List.of(), Set.of(), false);

    /** Span attribute on every span of a trace that the collector must route to tail sampling. */
    public static final String ATTR_TAIL = "gravitee.sampling.tail";
    /** Span attribute on the root span: whether the trace meets the tail criteria. */
    public static final String ATTR_KEEP = "gravitee.sampling.keep";

    public SamplingConfig {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException("Sampling ratio must be between 0 and 1, got " + ratio);
        }
        methods = methods == null ? Set.of() : Set.copyOf(methods);
        pathPrefixes = pathPrefixes == null ? List.of() : List.copyOf(pathPrefixes);
        keepStatuses = keepStatuses == null ? Set.of() : Set.copyOf(keepStatuses);
    }

    public boolean matches(final String method, final String path) {
        final boolean methodMatches = methods.isEmpty() || (method != null && methods.contains(method));
        final boolean pathMatches = pathPrefixes.isEmpty() || (path != null && pathPrefixes.stream().anyMatch(path::startsWith));
        return methodMatches && pathMatches;
    }

    public boolean hasTailCriteria() {
        return keepErrors || !keepStatuses.isEmpty();
    }

    public boolean keeps(final int status, final boolean failed) {
        return keepStatuses.contains(status) || (keepErrors && (failed || status >= 500));
    }
}
