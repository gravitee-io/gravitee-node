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
package io.gravitee.node.api.opentelemetry;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Span {
    /**
     * @return <code>true</code> if the span is the root (top span of the current context), <code>false</code> otherwise.
     */
    boolean isRoot();

    /**
     * @return traceparent value.
     */
    String getTraceparent();

    /**
     * Append custom attribute to the span while it is already created.
     *
     * @return current {@link Span}
     */
    <T> Span withAttribute(final String name, final T value);
}
