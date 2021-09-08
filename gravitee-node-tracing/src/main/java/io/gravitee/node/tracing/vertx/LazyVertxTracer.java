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
package io.gravitee.node.tracing.vertx;

import io.gravitee.node.api.tracing.Tracer;
import io.gravitee.node.tracing.TracingService;
import io.vertx.core.Context;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingPolicy;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LazyVertxTracer
  implements VertxTracer<Object, Object>, TracingService.TracerListener {

  private io.gravitee.node.tracing.vertx.VertxTracer<Object, Object> tracer;

  @Override
  public <R> Object receiveRequest(
    Context context,
    SpanKind kind,
    TracingPolicy policy,
    R request,
    String operation,
    Iterable<Map.Entry<String, String>> headers,
    TagExtractor<R> tagExtractor
  ) {
    if (tracer != null) {
      return tracer.receiveRequest(
        context,
        kind,
        policy,
        request,
        operation,
        headers,
        tagExtractor
      );
    }

    return request;
  }

  @Override
  public <R> void sendResponse(
    Context context,
    R response,
    Object payload,
    Throwable failure,
    TagExtractor<R> tagExtractor
  ) {
    if (tracer != null) {
      tracer.sendResponse(context, response, payload, failure, tagExtractor);
    }
  }

  @Override
  public <R> Object sendRequest(
    Context context,
    SpanKind kind,
    TracingPolicy policy,
    R request,
    String operation,
    BiConsumer<String, String> headers,
    TagExtractor<R> tagExtractor
  ) {
    if (tracer != null) {
      return tracer.sendRequest(
        context,
        kind,
        policy,
        request,
        operation,
        headers,
        tagExtractor
      );
    }

    return request;
  }

  @Override
  public <R> void receiveResponse(
    Context context,
    R response,
    Object payload,
    Throwable failure,
    TagExtractor<R> tagExtractor
  ) {
    if (tracer != null) {
      tracer.receiveResponse(context, response, payload, failure, tagExtractor);
    }
  }

  @Override
  public void close() {
    if (tracer != null) {
      tracer.close();
    }
  }

  @Override
  public void onRegister(Tracer tracer) {
    if (tracer instanceof io.gravitee.node.tracing.vertx.VertxTracer) {
      this.tracer =
        (io.gravitee.node.tracing.vertx.VertxTracer<Object, Object>) tracer;
    }
  }
}
