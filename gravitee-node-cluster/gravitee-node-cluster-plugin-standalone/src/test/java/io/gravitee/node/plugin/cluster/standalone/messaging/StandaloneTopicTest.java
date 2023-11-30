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
class StandaloneTopicTest {

    public static final String TOPIC_NAME = "topicName";
    private StandaloneTopic<String> cut;

    @BeforeEach
    public void beforeEach(Vertx vertx) {
        vertx.eventBus().registerCodec(new StandaloneMessageCodec());
        cut = new StandaloneTopic<>(vertx, TOPIC_NAME);
    }

    @Test
    void should_publish_event_to_topic(Vertx vertx, VertxTestContext testContext) {
        Checkpoint itemReceived = testContext.checkpoint();
        vertx
            .eventBus()
            .<String>localConsumer(TOPIC_NAME)
            .handler(event ->
                vertx.executeBlocking(
                    (Handler<Promise<Void>>) promise -> {
                        itemReceived.flag();
                        assertThat(event.body()).isEqualTo("message");
                        promise.handle(null);
                    }
                )
            );

        cut.publish("message");
    }

    @Test
    void should_receive_event_on_all_listeners(VertxTestContext testContext) {
        Checkpoint allListeners = testContext.checkpoint(2);
        cut.addMessageListener(message -> allListeners.flag());
        cut.addMessageListener(message -> allListeners.flag());
        cut.publish("message");
    }
}
