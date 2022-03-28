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
package io.gravitee.node.cache.standalone;

import static org.junit.Assert.*;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class StandaloneCacheTest {

    private static final String CACHE_NAME = "StandaloneCacheTest";
    private static final Long TIME_TO_LIVE = 1L;
    private static final String TEST_KEY = "foobar";
    private static final String TEST_VALUE = "value";

    @InjectMocks
    StandaloneCacheManager cacheManager;

    @Test
    public void shouldPutToCache() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(2);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.MILLISECONDS);
        cache.put(TEST_KEY + 2, TEST_VALUE, 2000, TimeUnit.MILLISECONDS);

        assertNotNull(cache.get(TEST_KEY));
        assertEquals(2, cache.size());
        assertFalse(cache.isEmpty());

        Thread.sleep(1000L);
        assertNotNull(cache.get(TEST_KEY + 2));
        //    assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }

    @Test
    public void shouldBeCleanedUpFromCache() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(100);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.put(TEST_KEY, TEST_VALUE, 10, TimeUnit.MILLISECONDS);

        Thread.sleep(12L);
        assertNull(cache.get(TEST_KEY));
    }

    @Test
    public void shouldBeRenewed() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(100);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        cache.put(TEST_KEY, TEST_VALUE, 40, TimeUnit.MILLISECONDS);
        Thread.sleep(30L);
        cache.get(TEST_KEY);
        Thread.sleep(30L);

        assertNotNull(cache.get(TEST_KEY));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());
    }
}
