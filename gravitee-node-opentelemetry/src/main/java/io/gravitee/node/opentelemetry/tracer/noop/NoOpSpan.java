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
package io.gravitee.node.opentelemetry.tracer.noop;

import io.gravitee.node.api.opentelemetry.Span;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class NoOpSpan implements Span {

    private final boolean root;

    public static Span asRoot() {
        return new NoOpSpan(true);
    }

    public static Span asDefault() {
        return new NoOpSpan(false);
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public <T> Span withAttribute(final String name, final T value) {
        return this;
    }
}