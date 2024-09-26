package com.graviteesource.services.runtimesecrets.storage;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.node.api.secrets.model.Secret;
import io.gravitee.node.api.secrets.runtime.storage.Cache;
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
        cut.put("dev", "secret_error", new Entry(Entry.Type.ERROR, null, "500"));
        cut.put("test", "secret_empty", new Entry(Entry.Type.EMPTY, null, "204"));
        cut.put("prod", "secret_not_found", new Entry(Entry.Type.NOT_FOUND, null, "404"));

        assertThat(cut.get("dev", "secret_error")).get().extracting("type", "error").containsExactly(Entry.Type.ERROR, "500");
        assertThat(cut.get("test", "secret_empty")).get().extracting("type", "error").containsExactly(Entry.Type.EMPTY, "204");
        assertThat(cut.get("prod", "secret_not_found"))
            .isPresent()
            .get()
            .extracting("type", "error")
            .containsExactly(Entry.Type.NOT_FOUND, "404");
    }

    @Test
    void should_store_segmented_data() {
        cut.put("dev", "secret", new Entry(Entry.Type.VALUE, Map.of("foo", new Secret("bar")), null));
        cut.put("test", "secret", new Entry(Entry.Type.VALUE, Map.of("buz", new Secret("puk")), null));
        assertThat(cut.get("dev", "secret"))
            .get()
            .usingRecursiveAssertion()
            .isEqualTo(new Entry(Entry.Type.VALUE, Map.of("foo", new Secret("bar")), null));
        assertThat(cut.get("test", "secret"))
            .get()
            .usingRecursiveAssertion()
            .isEqualTo(new Entry(Entry.Type.VALUE, Map.of("buz", new Secret("puk")), null));
    }

    @Test
    void should_perform_crud_ops() {
        cut.put(
            "dev",
            "secret",
            new Entry(Entry.Type.VALUE, Map.of("redis-password", new Secret("123456"), "ldap-password", new Secret("azerty")), null)
        );
        assertThat(cut.get("dev", "secret"))
            .get()
            .extracting(entry -> entry.value().values().stream().map(Secret::asString).toList())
            .asInstanceOf(InstanceOfAssertFactories.LIST)
            .containsExactlyInAnyOrder("123456", "azerty");
        assertThat(cut.get("dev", "secret"))
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
        cut.put("dev", "secret", dbPasswords);
        dbPasswordsAssert(cut.get("dev", "secret"));

        // no override as does not exists
        cut.computeIfAbsent("dev", "secret", () -> new Entry(Entry.Type.VALUE, Map.of(), null));
        dbPasswordsAssert(cut.get("dev", "secret"));

        // eviction
        cut.evict("dev", "secret");
        assertThat(cut.get("dev", "secret")).isNotPresent();

        cut.computeIfAbsent("dev", "secret", () -> dbPasswords);
        dbPasswordsAssert(cut.get("dev", "secret"));
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
