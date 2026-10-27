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
import static org.mockito.Mockito.mock;

import io.vertx.core.net.NetServer;
import org.junit.jupiter.api.Test;

class LimitedNetServerTest {

    private final NetServer delegate = mock(NetServer.class);

    @Test
    void should_equal_itself() {
        final LimitedNetServer server = new LimitedNetServer(delegate, new ConnectionLimiter(1), address -> false, null);

        assertThat(server).isEqualTo(server).hasSameHashCodeAs(server);
    }

    @Test
    void should_not_equal_a_wrapper_over_the_same_delegate_with_another_limiter() {
        final LimitedNetServer first = new LimitedNetServer(delegate, new ConnectionLimiter(1), address -> false, null);
        final LimitedNetServer second = new LimitedNetServer(delegate, new ConnectionLimiter(2), address -> false, null);

        assertThat(first).isNotEqualTo(second);
    }
}
