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
package io.gravitee.node.management.http.node.thread;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ThreadDumpEndpoint implements ManagementEndpoint {

    private final Logger LOGGER = LoggerFactory.getLogger(ThreadDumpEndpoint.class);

    private final PlainTextThreadDumpFormatter plainTextFormatter = new PlainTextThreadDumpFormatter();

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/threaddump";
    }

    @Override
    public void handle(RoutingContext context) {
        context
            .vertx()
            .executeBlocking(
                new Handler<Promise<String>>() {
                    @Override
                    public void handle(Promise<String> promise) {
                        try {
                            ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                            String dump = plainTextFormatter.format(threadInfos);

                            promise.complete(dump);
                        } catch (Exception ex) {
                            promise.fail(ex);
                        }
                    }
                },
                new Handler<AsyncResult<String>>() {
                    @Override
                    public void handle(AsyncResult<String> threadDumpResult) {
                        if (threadDumpResult.succeeded()) {
                            context
                                .response()
                                .setStatusCode(HttpStatusCode.OK_200)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8")
                                .setChunked(true)
                                .send(threadDumpResult.result());
                        } else {
                            LOGGER.error("Unable to generate thread dump.", threadDumpResult.cause());
                            context
                                .response()
                                .setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                                .setChunked(true)
                                .send(threadDumpResult.cause().getMessage());
                        }
                    }
                }
            );
    }
}
