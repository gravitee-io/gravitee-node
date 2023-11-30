/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)&#10;&#10;Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);&#10;you may not use this file except in compliance with the License.&#10;You may obtain a copy of the License at&#10;&#10;        http://www.apache.org/licenses/LICENSE-2.0&#10;&#10;Unless required by applicable law or agreed to in writing, software&#10;distributed under the License is distributed on an &quot;AS IS&quot; BASIS,&#10;WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.&#10;See the License for the specific language governing permissions and&#10;limitations under the License.
 */

package io.gravitee.node.plugin.cluster.standalone.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(VertxExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StandaloneQueueTest {

    public static final String QUEUE_NAME = "queueName";
    private StandaloneQueue<String> cut;

    @BeforeEach
    public void beforeEach(Vertx vertx) {
        vertx.eventBus().registerCodec(new StandaloneMessageCodec());
        cut = new StandaloneQueue<>(vertx, QUEUE_NAME);
    }

    @Test
    void should_add_item_to_queue(Vertx vertx, VertxTestContext testContext) {
        Checkpoint itemReceived = testContext.checkpoint();
        vertx
            .eventBus()
            .<String>localConsumer(QUEUE_NAME)
            .handler(event ->
                vertx.executeBlocking(
                    (Handler<Promise<Void>>) promise -> {
                        itemReceived.flag();
                        assertThat(event.body()).isEqualTo("message");
                        promise.handle(null);
                    }
                )
            );

        cut.add("message");
    }

    @Test
    void should_receive_item_only_once(VertxTestContext testContext) {
        Checkpoint oneListenerOnly = testContext.checkpoint();
        cut.addMessageListener(message -> oneListenerOnly.flag());
        cut.addMessageListener(message -> oneListenerOnly.flag());
        cut.add("message");
    }
}
