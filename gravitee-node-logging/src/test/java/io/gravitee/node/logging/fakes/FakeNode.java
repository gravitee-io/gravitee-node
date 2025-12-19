package io.gravitee.node.logging.fakes;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.node.api.Node;

/**
 * Minimal Node implementation for tests.
 */
public record FakeNode(String id, String hostname) implements Node {
    @Override
    public String name() {
        return "fake";
    }

    @Override
    public String application() {
        return "fake-app";
    }

    @Override
    public Lifecycle.State lifecycleState() {
        return Lifecycle.State.STARTED;
    }

    @Override
    public Node start() throws Exception {
        return this;
    }

    @Override
    public Node preStop() throws Exception {
        return this;
    }

    @Override
    public Node stop() throws Exception {
        return this;
    }
}
