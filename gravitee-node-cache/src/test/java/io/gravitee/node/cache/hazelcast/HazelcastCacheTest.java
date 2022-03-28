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
package io.gravitee.node.cache.hazelcast;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheConfiguration;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HazelcastCacheTest {

    private static final String CACHE_NAME = "HazelcastCacheTest";
    private static final Long TIME_TO_LIVE = 60L;
    private static final String TEST_KEY = "foobar";
    private static final String TEST_VALUE = "value";

    Config config;

    @Mock
    HazelcastInstance hazelcastInstance;

    @Mock
    IMap<Object, Object> map;

    @Mock
    ApplicationContext applicationContext;

    @InjectMocks
    HazelcastCacheManager cacheManager;

    @Before
    public void setup() throws Exception {
        config = new Config();
        when(hazelcastInstance.getMap(anyString())).thenReturn(map);

        MapConfig mapConfig = new MapConfig();
        mapConfig.setName("cache_*");
        mapConfig.setTimeToLiveSeconds(600);
        mapConfig.setMaxIdleSeconds(600);
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(200);
        mapConfig.setEvictionConfig(evictionConfig);

        config.addMapConfig(mapConfig);
    }

    @Test
    public void shouldPutToCache() {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(60);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        cache.put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);

        verify(hazelcastInstance, times(1)).getMap(argThat(cacheName -> cacheName.startsWith(CACHE_NAME)));
        verify(map, times(1)).put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldPutToCacheWithTtl() {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(60);
        Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        cache.put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);

        verify(hazelcastInstance, times(1)).getMap(argThat(cacheName -> cacheName.startsWith(CACHE_NAME)));
        verify(map, times(1)).put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldDestroyCache() throws Exception {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setTimeToLiveSeconds(60);
        final Cache<String, String> cache = cacheManager.getOrCreateCache(CACHE_NAME, configuration);
        assertNotNull(cache);
        cache.put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);

        verify(hazelcastInstance, times(1)).getMap(argThat(cacheName -> cacheName.startsWith(CACHE_NAME)));
        verify(map, times(1)).put(TEST_KEY, TEST_VALUE, 30, TimeUnit.MILLISECONDS);

        cache.clear();
    }
}
