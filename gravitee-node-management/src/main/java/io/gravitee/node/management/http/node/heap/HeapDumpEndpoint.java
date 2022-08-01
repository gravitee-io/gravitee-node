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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HeapDumpEndpoint implements ManagementEndpoint {

    private final Logger LOGGER = LoggerFactory.getLogger(HeapDumpEndpoint.class);

    private final long timeout = TimeUnit.SECONDS.toMillis(10);

    private final Lock lock = new ReentrantLock();

    private HeapDumpSupplier heapDumpSupplier;

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }

    @Override
    public String path() {
        return "/heapdump";
    }

    @Override
    public void handle(RoutingContext ctx) {
        final HttpServerResponse response = ctx.response();
        final boolean live = Boolean.parseBoolean(ctx.request().getParam("live", Boolean.FALSE.toString()));

        ctx
            .vertx()
            .executeBlocking(
                new Handler<Promise<File>>() {
                    @Override
                    public void handle(Promise<File> promise) {
                        try {
                            if (HeapDumpEndpoint.this.lock.tryLock(HeapDumpEndpoint.this.timeout, TimeUnit.MILLISECONDS)) {
                                try {
                                    File dumpFile = dump(live);
                                    promise.complete(dumpFile);
                                } catch (Exception e) {
                                    promise.fail(e);
                                }
                            }
                        } catch (InterruptedException ex) {
                            promise.fail(ex);
                        } finally {
                            HeapDumpEndpoint.this.lock.unlock();
                        }
                    }
                },
                new Handler<AsyncResult<File>>() {
                    @Override
                    public void handle(AsyncResult<File> fileAsyncResult) {
                        if (fileAsyncResult.succeeded()) {
                            File file = fileAsyncResult.result();
                            response
                                .setStatusCode(HttpStatusCode.OK_200)
                                .setChunked(true)
                                .sendFile(file.getAbsolutePath())
                                .onComplete(
                                    new Handler<AsyncResult<Void>>() {
                                        @Override
                                        public void handle(AsyncResult<Void> voidAsyncResult) {
                                            try {
                                                Files.delete(file.toPath());
                                            } catch (IOException ex) {
                                                LOGGER.warn("Failed to delete temporary heap dump file '" + file.toPath() + "'", ex);
                                            }
                                        }
                                    }
                                );
                        } else {
                            LOGGER.error("Unable to generate heap dump.", fileAsyncResult.cause());
                            response
                                .setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                                .setChunked(true)
                                .send(fileAsyncResult.cause().getMessage());
                        }
                    }
                }
            );
    }

    private File dump(boolean live) throws IOException, InterruptedException {
        if (this.heapDumpSupplier == null) {
            this.heapDumpSupplier = createHeapDumpSupplier();
        }

        File file = createTempFile();
        this.heapDumpSupplier.dump(file, live);
        return file;
    }

    private File createTempFile() throws IOException {
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(LocalDateTime.now());
        File file = File.createTempFile("heap-" + date, "." + determineDumpSuffix());
        file.delete();
        return file;
    }

    protected HeapDumpSupplier createHeapDumpSupplier() throws HeapDumpException {
        try {
            return new HotSpotHeapDumpSupplier();
        } catch (HeapDumpException ex) {
            return new OpenJ9HeapDumpSupplier();
        }
    }

    private String determineDumpSuffix() {
        if (this.heapDumpSupplier instanceof OpenJ9HeapDumpSupplier) {
            return "phd";
        }
        return "hprof";
    }
}
