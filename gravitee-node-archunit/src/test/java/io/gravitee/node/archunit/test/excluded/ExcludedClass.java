package io.gravitee.node.archunit.test.excluded;

import org.slf4j.LoggerFactory;

public class ExcludedClass {

    public void log() {
        LoggerFactory.getLogger(ExcludedClass.class).info("Should be excluded");
    }
}
