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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
import io.gravitee.node.api.secrets.runtime.storage.CacheKey;
import io.gravitee.node.api.secrets.runtime.storage.Entry;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SimpleOffHeapCacheTest {

    private final Cache cut = new SimpleOffHeapCache();

    @Test
    void should_store_error_entries() {
        cut.put(new CacheKey("dev", "secret_error"), new Entry(Entry.Type.ERROR, null, "500"));
        cut.put(new CacheKey("test", "secret_empty"), new Entry(Entry.Type.EMPTY, null, "204"));
        cut.put(new CacheKey("prod", "secret_not_found"), new Entry(Entry.Type.NOT_FOUND, null, "404"));

        assertThat(cut.get(new CacheKey("dev", "secret_error"))).get().extracting("type", "error").containsExactly(Entry.Type.ERROR, "500");
        assertThat(cut.get(new CacheKey("test", "secret_empty")))
            .get()
            .extracting("type", "error")
            .containsExactly(Entry.Type.EMPTY, "204");
        assertThat(cut.get(new CacheKey("prod", "secret_not_found")))
            .isPresent()
            .get()
            .extracting("type", "error")
            .containsExactly(Entry.Type.NOT_FOUND, "404");
    }

    @Test
    void should_store_segmented_data() {
        cut.put(new CacheKey("dev", "secret"), new Entry(Entry.Type.VALUE, Map.of("foo", new Secret("bar")), null));
        cut.put(new CacheKey("test", "secret"), new Entry(Entry.Type.VALUE, Map.of("buz", new Secret("puk")), null));
        assertThat(cut.get(new CacheKey("dev", "secret")))
            .get()
            .usingRecursiveAssertion()
            .isEqualTo(new Entry(Entry.Type.VALUE, Map.of("foo", new Secret("bar")), null));
        assertThat(cut.get(new CacheKey("test", "secret")))
            .get()
            .usingRecursiveAssertion()
            .isEqualTo(new Entry(Entry.Type.VALUE, Map.of("buz", new Secret("puk")), null));
    }

    @Test
    void should_perform_crud_ops() {
        cut.put(
            new CacheKey("dev", "secret"),
            new Entry(Entry.Type.VALUE, Map.of("redis-password", new Secret("123456"), "ldap-password", new Secret("azerty")), null)
        );
        assertThat(cut.get(new CacheKey("dev", "secret")))
            .get()
            .extracting(entry -> entry.value().values().stream().map(Secret::asString).toList())
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactlyInAnyOrder("123456", "azerty");
        assertThat(cut.get(new CacheKey("dev", "secret")))
            .get()
            .extracting(entry -> entry.value().keySet())
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .containsExactlyInAnyOrder("redis-password", "ldap-password");

        // override and test it was really done
        Entry dbPasswords = new Entry(
            Entry.Type.VALUE,
            Map.of("mongodb-password", new Secret("778899"), "mysql-password", new Secret("qwerty")),
            null
        );
        cut.put(new CacheKey("dev", "secret"), dbPasswords);
        dbPasswordsAssert(cut.get(new CacheKey("dev", "secret")));

        // no override as does not exists
        cut.computeIfAbsent(new CacheKey("dev", "secret"), () -> new Entry(Entry.Type.VALUE, Map.of(), null));
        dbPasswordsAssert(cut.get(new CacheKey("dev", "secret")));

        // eviction
        cut.evict(new CacheKey("dev", "secret"));
        assertThat(cut.get(new CacheKey("dev", "secret"))).isNotPresent();

        cut.computeIfAbsent(new CacheKey("dev", "secret"), () -> dbPasswords);
        dbPasswordsAssert(cut.get(new CacheKey("dev", "secret")));
    }

    private void dbPasswordsAssert(Optional<Entry> optEntry) {
        assertThat(optEntry)
            .get()
            .extracting(entry -> entry.value().values().stream().map(Secret::asString).toList())
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactlyInAnyOrder("778899", "qwerty");
        assertThat(optEntry)
            .get()
            .extracting(entry -> entry.value().keySet())
            .asInstanceOf(InstanceOfAssertFactories.COLLECTION)
            .containsExactlyInAnyOrder("mongodb-password", "mysql-password");
    }
}
