package io.gravitee.node.management.http.vertx.endpoint;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.node.management.http.endpoint.ManagementEndpoint;
import io.vertx.ext.web.RoutingContext;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultManagementEndpointManagerTest {

    private DefaultManagementEndpointManager cut;

    @BeforeEach
    void init() {
        cut = new DefaultManagementEndpointManager();
    }

    @Nested
    class RegisterEndpoint {

        @Test
        void should_notify_listener_when_registering_endpoint() {
            final FakeListener listener = spy(new FakeListener());
            final FakeManagementEndpoint managementEndpoint = new FakeManagementEndpoint();

            cut.onEndpointRegistered(listener);
            cut.register(managementEndpoint);

            verify(listener).accept(managementEndpoint);
        }

        @Test
        void should_notify_all_listeners_when_registering_endpoint() {
            final FakeListener listener1 = spy(new FakeListener());
            final FakeListener listener2 = spy(new FakeListener());
            final FakeManagementEndpoint managementEndpoint = new FakeManagementEndpoint();

            cut.onEndpointRegistered(listener1);
            cut.onEndpointRegistered(listener2);
            cut.register(managementEndpoint);

            verify(listener1).accept(managementEndpoint);
            verify(listener2).accept(managementEndpoint);
        }

        @Test
        void should_notify_listener_with_all_existing_endpoints_when_listener_subscribes() {
            final FakeListener listener = spy(new FakeListener());
            final FakeManagementEndpoint managementEndpoint1 = new FakeManagementEndpoint();
            final FakeManagementEndpoint managementEndpoint2 = new FakeManagementEndpoint();

            cut.register(managementEndpoint1);
            cut.register(managementEndpoint2);

            // Listener registered after endpoints have been registered.
            cut.onEndpointRegistered(listener);

            verify(listener).accept(managementEndpoint1);
            verify(listener).accept(managementEndpoint2);
        }
    }

    @Nested
    class UnregisterEndpoint {

        @Test
        void should_notify_listener_when_unregistering_endpoint() {
            final FakeListener listener = spy(new FakeListener());
            final FakeManagementEndpoint managementEndpoint = new FakeManagementEndpoint();

            cut.register(managementEndpoint);
            cut.onEndpointUnregistered(listener);
            cut.unregister(managementEndpoint);

            verify(listener).accept(managementEndpoint);
        }
    }

    private static class FakeManagementEndpoint implements ManagementEndpoint {

        @Override
        public HttpMethod method() {
            return null;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public void handle(RoutingContext context) {}
    }

    private static class FakeListener implements Consumer<ManagementEndpoint> {

        @Override
        public void accept(ManagementEndpoint managementEndpoint) {}
    }
}
