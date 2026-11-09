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
package io.gravitee.node.api.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class CacheConfiguration {

    @Builder.Default
    private boolean distributed = false;

    @Builder.Default
    private long maxSize = -1;

    /**
     * How long an entry lives after it is written, for entries written without their own time to live. 0 or less for none.
     * An entry's own time to live can't be bigger.
     */
    @Builder.Default
    private long timeToLiveInMs = -1;

    /**
     * How long an entry lives without being read. 0 or less for none.
     *
     * <p>The in-memory and Hazelcast caches combine it with {@link #timeToLiveInMs}: an entry goes at the earliest of
     * the two. The Redis cache has no idle expiry and emulates it, which makes two differences:
     * <ul>
     *   <li>when the cache also has a time to live, only the time to live applies, so an entry that is never read stays
     *       until then;</li>
     *   <li>when the cache has a time to idle only, an entry written with its own time to live expires at the earliest of
     *       both, but each read pushes its expiry back by the idle time, possibly past its own time to live.</li>
     * </ul>
     * Hazelcast tracks idle time with a one-second resolution: an idle time under two seconds may drop entries that are
     * read in time.
     */
    @Builder.Default
    private long timeToIdleInMs = -1;
}
