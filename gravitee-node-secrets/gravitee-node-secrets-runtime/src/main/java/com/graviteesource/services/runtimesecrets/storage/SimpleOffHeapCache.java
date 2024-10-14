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
package com.graviteesource.services.runtimesecrets.storage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SimpleOffHeapCache implements Cache {

    private final Kryo kryo;

    public SimpleOffHeapCache() {
        this.kryo = new Kryo();
        kryo.register(Secret.class);
        kryo.register(Entry.class);
        kryo.register(Entry.Type.class);
        kryo.register(HashMap.class);
    }

    private final ConcurrentMap<CacheKey, ByteBuffer> data = new ConcurrentHashMap<>();

    @Override
    public void put(CacheKey cacheKey, Entry value) {
        var bytes = serialize(value);
        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        data.put(cacheKey, byteBuffer);
        byteBuffer.put(bytes);
    }

    @Override
    public void putPartial(CacheKey cacheKey, Map<String, Secret> partial) {
        Optional<Entry> entryOpt = this.get(cacheKey);
        Entry entry;
        if (entryOpt.isPresent()) {
            entry = entryOpt.get();
            if (entry.type() == Entry.Type.VALUE) {
                Map<String, Secret> secretMap = entry.value();
                secretMap.putAll(partial);
                entry = new Entry(Entry.Type.VALUE, secretMap, null);
            } else {
                entry = new Entry(Entry.Type.VALUE, partial, null);
            }
        } else {
            entry = new Entry(Entry.Type.VALUE, partial, null);
        }
        this.put(cacheKey, entry);
    }

    @Override
    public Optional<Entry> get(CacheKey cacheKey) {
        ByteBuffer byteBuffer = data.get(cacheKey);
        if (byteBuffer != null) {
            byte[] buf = new byte[byteBuffer.limit()];
            byteBuffer.position(0);
            byteBuffer.get(buf, 0, buf.length);
            return Optional.of(deserialize(buf));
        }
        return Optional.empty();
    }

    @Override
    public void computeIfAbsent(CacheKey cacheKey, Supplier<Entry> supplier) {
        data.computeIfAbsent(
            cacheKey,
            key -> {
                Entry value = supplier.get();
                byte[] stringAsBytes = serialize(value);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(stringAsBytes.length);
                byteBuffer.put(stringAsBytes);
                return byteBuffer;
            }
        );
    }

    @Override
    public void evict(CacheKey cacheKey) {
        data.remove(cacheKey);
    }

    public byte[] serialize(Entry value) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); Output out = new Output(bytes)) {
            this.kryo.writeObject(out, value);
            return out.toBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Entry deserialize(byte[] bytes) {
        try (Input in = new Input(bytes)) {
            return this.kryo.readObject(in, Entry.class);
        }
    }
}
