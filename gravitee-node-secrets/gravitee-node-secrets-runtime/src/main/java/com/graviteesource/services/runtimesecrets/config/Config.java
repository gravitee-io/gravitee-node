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
package com.graviteesource.services.runtimesecrets.config;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Config(OnTheFlySpecs onTheFlySpecs, Retry retry, Renewal renewal) {
    public static final String CONFIG_PREFIX = "api.secrets";
    public static final String ON_THE_FLY_SPECS_ENABLED = CONFIG_PREFIX + ".onTheFlySpecs.enabled";
    public static final String ON_THE_FLY_SPECS_ON_ERROR_RETRY_AFTER_DELAY = CONFIG_PREFIX + ".onTheFlySpecs.onErrorRetryAfter.delay";
    public static final String ON_THE_FLY_SPECS_ON_ERROR_RETRY_AFTER_UNIT = CONFIG_PREFIX + ".onTheFlySpecs.onErrorRetryAfter.unit";
    public static final String RENEWAL_ENABLED = CONFIG_PREFIX + ".renewal.enable";
    public static final String RENEWAL_CHECK_DELAY = CONFIG_PREFIX + ".renewal.check.delay";
    public static final String RENEWAL_CHECK_UNIT = CONFIG_PREFIX + ".renewal.check.unit";
    public static final String RETRY_ON_ERROR_ENABLED = CONFIG_PREFIX + ".retryOnError.enable";
    public static final String RETRY_ON_ERROR_DELAY = CONFIG_PREFIX + ".retryOnError.delay";
    public static final String RETRY_ON_ERROR_UNIT = CONFIG_PREFIX + ".retryOnError.unit";
    public static final String RETRY_ON_ERROR_BACKOFF_FACTOR = CONFIG_PREFIX + ".retryOnError.backoffFactor";
    public static final String RETRY_ON_ERROR_BACKOFF_MAX_DELAY = CONFIG_PREFIX + ".retryOnError.maxDelay";
    public static final String API_SECRETS_ALLOW_PROVIDERS_FROM_CONFIGURATION = CONFIG_PREFIX + ".allowProvidersFromConfiguration";
}
