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
package io.gravitee.node.vertx.server.tcp;

import io.vertx.core.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts the connections held by a TCP server and refuses new ones once a maximum is reached.
 * <p>
 * One instance is shared by every {@code NetServer} created by the same {@link VertxTcpServer}, so that several
 * listeners (e.g. one port per broker) draw from a single budget.
 */
final class ConnectionLimiter {

    private final int limit;
    private final AtomicInteger count = new AtomicInteger();

    ConnectionLimiter(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.limit = limit;
    }

    /**
     * Takes a slot if one is free. The count never exceeds the limit, even transiently.
     */
    boolean tryAcquire() {
        int current;
        do {
            current = count.get();
            if (current >= limit) {
                return false;
            }
        } while (!count.compareAndSet(current, current + 1));
        return true;
    }

    void release() {
        count.decrementAndGet();
    }

    int count() {
        return count.get();
    }

    int limit() {
        return limit;
    }

    /**
     * Connections from the loopback addresses are never counted nor refused: they are the pod's own probes.
     * Only the two literal addresses are exempt, not the whole 127.0.0.0/8 range, so that a service-mesh sidecar
     * forwarding client traffic from another 127.x.x.x address is still counted.
     */
    static boolean isLoopback(SocketAddress address) {
        if (address == null || address.isDomainSocket()) {
            return false;
        }
        final String host = address.host();
        return "127.0.0.1".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host);
    }
}
