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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.vertx.core.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConnectionLimiterTest {

    @Test
    void should_refuse_once_the_limit_is_reached_and_admit_again_after_a_release() {
        final ConnectionLimiter cut = new ConnectionLimiter(2);

        assertThat(cut.tryAcquire()).isTrue();
        assertThat(cut.tryAcquire()).isTrue();
        assertThat(cut.tryAcquire()).isFalse();
        assertThat(cut.count()).isEqualTo(2);

        cut.release();

        assertThat(cut.tryAcquire()).isTrue();
        assertThat(cut.count()).isEqualTo(2);
    }

    @Test
    void should_reject_a_non_positive_limit() {
        assertThatThrownBy(() -> new ConnectionLimiter(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_never_hold_more_slots_than_the_limit_under_contention() throws Exception {
        final int limit = 3;
        final int threads = 16;
        final ConnectionLimiter cut = new ConnectionLimiter(limit);
        final AtomicInteger highest = new AtomicInteger();
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CyclicBarrier barrier = new CyclicBarrier(threads);
        try {
            final List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(
                    pool.submit(() -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        for (int i = 0; i < 200_000; i++) {
                            if (cut.tryAcquire()) {
                                highest.accumulateAndGet(cut.count(), Math::max);
                                cut.release();
                            }
                        }
                        return null;
                    })
                );
            }
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(highest.get()).as("highest count observed by a holder").isBetween(1, limit);
        assertThat(cut.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.1", "::1", "0:0:0:0:0:0:0:1" })
    void should_exempt_the_loopback_addresses(String host) {
        assertThat(ConnectionLimiter.isLoopback(SocketAddress.inetSocketAddress(4000, host))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "127.0.0.6", "127.0.0.2", "10.0.0.1", "203.0.113.9", "fe80::1" })
    void should_count_every_other_address_including_the_rest_of_127_0_0_0_8(String host) {
        assertThat(ConnectionLimiter.isLoopback(SocketAddress.inetSocketAddress(4000, host))).isFalse();
    }

    @Test
    void should_count_an_unknown_or_domain_socket_address() {
        assertThat(ConnectionLimiter.isLoopback(null)).isFalse();
        assertThat(ConnectionLimiter.isLoopback(SocketAddress.domainSocketAddress("/tmp/x.sock"))).isFalse();
    }
}
