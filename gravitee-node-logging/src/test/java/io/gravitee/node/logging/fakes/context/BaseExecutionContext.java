package io.gravitee.node.logging.fakes.context;

import org.slf4j.Logger;

public interface BaseExecutionContext {
    String getAttribute(String key);

    default Logger withLogger(Logger logger) {
        return logger;
    }
}
