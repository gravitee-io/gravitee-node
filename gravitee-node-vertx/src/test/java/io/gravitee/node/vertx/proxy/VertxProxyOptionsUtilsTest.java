package io.gravitee.node.vertx.proxy;

import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.PROXY_HOST_PROPERTY;
import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.PROXY_PASSWORD_PROPERTY;
import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.PROXY_PORT_PROPERTY;
import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.PROXY_TYPE_PROPERTY;
import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.PROXY_USERNAME_PROPERTY;
import static io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils.buildProxyOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxProxyOptionsUtilsTest {

    @Test
    void shouldCreateFromProperties() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("3128");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("HTTP");

        final ProxyOptions expectedProxyOptions = new ProxyOptions();

        final var result = buildProxyOptions(configuration);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedProxyOptions);
    }

    @Test
    void shouldCreateProxyOptionsWithUserNameAndPassword() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("3128");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("HTTP");
        when(configuration.getProperty(PROXY_USERNAME_PROPERTY)).thenReturn("gravitee");
        when(configuration.getProperty(PROXY_PASSWORD_PROPERTY)).thenReturn("gravitee");

        final ProxyOptions expectedProxyOptions = new ProxyOptions();
        expectedProxyOptions.setUsername("gravitee");
        expectedProxyOptions.setPassword("gravitee");

        final var result = buildProxyOptions(configuration);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedProxyOptions);
    }

    @Test
    void shouldSupportSocks4() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("4145");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("SOCKS4");

        final ProxyOptions expectedProxyOptions = new ProxyOptions();
        expectedProxyOptions.setPort(4145);
        expectedProxyOptions.setType(ProxyType.SOCKS4);

        final var result = buildProxyOptions(configuration);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedProxyOptions);
    }

    @Test
    void shouldSupportSocks5() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("1080");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("SOCKS5");

        final ProxyOptions expectedProxyOptions = new ProxyOptions();
        expectedProxyOptions.setPort(1080);
        expectedProxyOptions.setType(ProxyType.SOCKS5);

        final var result = buildProxyOptions(configuration);

        assertThat(result).usingRecursiveComparison().isEqualTo(expectedProxyOptions);
    }

    @Test
    void shouldNotCreateProxyOptionsBecauseNoHost() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("4145");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("SOCKS4");

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("system.proxy.host: Proxy host may not be null");
    }

    @Test
    void shouldNotCreateProxyOptionsBecauseNoPort() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("HTTP");

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("system.proxy.port: Proxy port may not be null");
    }

    @Test
    void shouldNotCreateProxyOptionsBecausePortIsNotANumber() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("1O24");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("HTTP");

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("system.proxy.port: For input string: \"1O24\"");
    }

    @Test
    void shouldNotCreateProxyOptionsBecausePortIsOutOfRange() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("65536");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("HTTP");

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("system.proxy.port: Invalid proxy port 65536");
    }

    @Test
    void shouldNotCreateProxyOptionsBecauseOfUnknownType() {
        final Configuration configuration = mock(Configuration.class);
        when(configuration.getProperty(PROXY_HOST_PROPERTY)).thenReturn("localhost");
        when(configuration.getProperty(PROXY_PORT_PROPERTY)).thenReturn("70");
        when(configuration.getProperty(PROXY_TYPE_PROPERTY)).thenReturn("GOPHER");

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("system.proxy.type: No enum constant io.vertx.core.net.ProxyType.GOPHER");
    }

    @Test
    void shouldAggregateErrorMessages() {
        final Configuration configuration = mock(Configuration.class);

        assertThatThrownBy(() -> buildProxyOptions(configuration))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(
                "system.proxy.host: Proxy host may not be null, system.proxy.port: Proxy port may not be null, system.proxy.type: Name is null"
            );
    }
}
