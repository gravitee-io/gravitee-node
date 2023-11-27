package io.gravitee.node.vertx.cert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class VertxTLSOptionsRegistryTest {

    @Mock
    KeyCertOptions keyCertOptions;

    @Mock
    KeyCertOptions keyCertOptions2;

    @Mock
    TrustOptions trustOptions;

    @Mock
    TrustOptions trustOptions2;

    VertxTLSOptionsRegistry cut = new VertxTLSOptionsRegistry();

    @Test
    void should_register_options() {
        assertThat(cut.lookupKeyCertOptions("foo")).isNull();
        assertThat(cut.lookupTrustOptions("foo")).isNull();

        cut.registerOptions("foo", keyCertOptions);
        cut.registerOptions("foo", trustOptions);
        cut.registerOptions("bar", keyCertOptions2);
        cut.registerOptions("bar", trustOptions2);

        assertThat(cut.lookupKeyCertOptions("foo")).isSameAs(keyCertOptions);
        assertThat(cut.lookupTrustOptions("foo")).isSameAs(trustOptions);
        assertThat(cut.lookupKeyCertOptions("bar")).isSameAs(keyCertOptions2);
        assertThat(cut.lookupTrustOptions("bar")).isSameAs(trustOptions2);

        // override should not work
        cut.registerOptions("foo", keyCertOptions2);
        cut.registerOptions("foo", trustOptions2);
        assertThat(cut.lookupKeyCertOptions("foo")).isSameAs(keyCertOptions);
        assertThat(cut.lookupTrustOptions("foo")).isSameAs(trustOptions);
    }

    @Test
    void should_fail_with_null_options() {
        assertThatCode(() -> cut.registerOptions("foo", (KeyCertOptions) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("options");
        assertThatCode(() -> cut.registerOptions("foo", (TrustOptions) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("options");
    }
}
