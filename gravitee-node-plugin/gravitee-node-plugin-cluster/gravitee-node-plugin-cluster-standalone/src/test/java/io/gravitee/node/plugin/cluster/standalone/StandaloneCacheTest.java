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
package io.gravitee.node.plugin.cluster.standalone;

import static org.junit.Assert.*;

import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class StandaloneCacheTest {

    private static final String CACHE_NAME = "StandaloneCacheTest";
    private static final String TEST_KEY = "key1";
    private static final String ANOTHER_TEST_KEY = "key2";
    private static final String TEST_VALUE = "value1";
    private static final String ANOTHER_TEST_VALUE = "value2";

    StandaloneCacheManager cacheManager;

    @Before
    public void setUp() {
        cacheManager = new StandaloneCacheManager();
    }

    @Test
    public void shouldPutToCache() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(2);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        cache.put(TEST_KEY, TEST_VALUE, 1, TimeUnit.MILLISECONDS);
        cache.put(ANOTHER_TEST_KEY, ANOTHER_TEST_VALUE, 2000, TimeUnit.MILLISECONDS);

        assertNotNull(cache.get(TEST_KEY));
        assertEquals(2, cache.size());
        assertFalse(cache.isEmpty());

        Thread.sleep(100);
        assertEquals(ANOTHER_TEST_VALUE, cache.get(ANOTHER_TEST_KEY));
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
    public void shouldRespectTimeToLive() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration(); // Global time to live is 10 second
        configuration.setTimeToLiveSeconds(10);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        // Specific time to live is 100 millisecond
        cache.put(TEST_KEY, TEST_VALUE, 100, TimeUnit.MILLISECONDS);

        assertEquals(TEST_VALUE, cache.get(TEST_KEY));

        Thread.sleep(20);
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));

        // Wait a bit more to be sure that time to live of 100ms is reached, and then check that value was removed from cache
        Thread.sleep(100);
        assertNull(cache.get(TEST_KEY));
    }

    @Test
    public void shouldUseGlobalTimeToLiveWhenSpecificTTLIs0() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        // Global time to live is 1 second
        configuration.setTimeToLiveSeconds(1);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration); // Specific time to live is 0 == infinite keeping
        cache.put(TEST_KEY, TEST_VALUE, 0, TimeUnit.MILLISECONDS);
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));

        Thread.sleep(20);
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));
        // Wait a bit more to be sure that global time to live of 1s is reached, and then check that value was removed from cache
        Thread.sleep(1000);
        assertNull(cache.get(TEST_KEY));
    }

    @Test
    public void shouldReturnItsName() {
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, new CacheConfiguration());

        assertEquals(CACHE_NAME, cache.getName());
    }

    @Test
    public void shouldRemoveEverythingWhenClearingCache() {
        CacheConfiguration configuration = new CacheConfiguration();
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.put(TEST_KEY, TEST_VALUE);
        cache.put(ANOTHER_TEST_KEY, ANOTHER_TEST_VALUE);
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));
        cache.clear();

        assertNull(cache.get(TEST_KEY));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void shouldEvictACacheEntry() {
        CacheConfiguration configuration = new CacheConfiguration();
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.put(TEST_KEY, TEST_VALUE);
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));

        cache.evict(TEST_KEY);

        assertNull(cache.get(TEST_KEY));
    }

    @Test
    public void shouldPutMultipleValues() {
        CacheConfiguration configuration = new CacheConfiguration();
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.putAll(Map.of(TEST_KEY, TEST_VALUE, ANOTHER_TEST_KEY, ANOTHER_TEST_VALUE));
        assertEquals(TEST_VALUE, cache.get(TEST_KEY));
        assertEquals(ANOTHER_TEST_VALUE, cache.get(ANOTHER_TEST_KEY));
    }

    @Test
    public void shouldGetAllValuesRespectingTTL() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        // Specific time to live is 100 millisecond
        cache.put(TEST_KEY, TEST_VALUE, 100, TimeUnit.MILLISECONDS);
        cache.put(ANOTHER_TEST_KEY, ANOTHER_TEST_VALUE, 0, TimeUnit.MILLISECONDS);

        assertEquals(List.of(TEST_VALUE, ANOTHER_TEST_VALUE), cache.values());
        Thread.sleep(100);

        assertEquals(List.of(ANOTHER_TEST_VALUE), cache.values());
    }

    @Test
    public void shouldRespectGlobalMaxSize() {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setMaxSize(2);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.put(TEST_KEY, TEST_VALUE);
        cache.put(ANOTHER_TEST_KEY, ANOTHER_TEST_VALUE);

        assertEquals(2, cache.size());
        assertEquals(List.of(TEST_VALUE, ANOTHER_TEST_VALUE), cache.values());

        cache.put("key3", "value3");
        assertEquals(2, cache.size());
        assertEquals(List.of("value3", ANOTHER_TEST_VALUE), cache.values());
    }

    @Test
    public void shouldRespectTimeToIdle() throws InterruptedException {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToIdleSeconds(1);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);

        cache.put(TEST_KEY, TEST_VALUE);

        assertEquals(TEST_VALUE, cache.get(TEST_KEY));
        // Wait a bit more to be sure that time to idle of 1s is reached, and then check that value was removed from cache
        Thread.sleep(1100);
        assertNull(cache.get(TEST_KEY));
    }
}
