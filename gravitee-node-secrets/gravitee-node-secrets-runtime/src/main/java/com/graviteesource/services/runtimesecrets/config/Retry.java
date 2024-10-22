package com.graviteesource.services.runtimesecrets.config;

import java.util.concurrent.TimeUnit;/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

public record Retry(boolean enabled, long delay, TimeUnit unit, float backoffFactor, int maxDelay) {
    public static Retry none() {
        return new Retry(false, 0, TimeUnit.MILLISECONDS, 0, 0);
    }
}
