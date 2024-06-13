package io.gravitee.node.monitoring.healthcheck;

import io.gravitee.node.api.healthcheck.Probe;
import io.gravitee.node.api.healthcheck.Result;
import java.util.concurrent.CompletableFuture;

class TestingProbe implements Probe {

    private final String id;
    private boolean isVisibleByDefault = true;

    public TestingProbe(String id) {
        this.id = id;
    }

    public TestingProbe(String id, boolean isVisibleByDefault) {
        this.id = id;
        this.isVisibleByDefault = isVisibleByDefault;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isVisibleByDefault() {
        return this.isVisibleByDefault;
    }

    @Override
    public CompletableFuture<Result> check() {
        return null;
    }
}
