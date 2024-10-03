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
public record Config(boolean onTheFlySpecsEnabled, long onTheFlySpecsDelayBeforeRetryMs, boolean allowEmptyACLSpecs) {
    public static final String CONFIG_PREFIX = "api.secrets";
    public static final String ALLOW_EMPTY_NO_ACL_SPECS = CONFIG_PREFIX + ".allowNoACLsSpecs";
    public static final String ON_THE_FLY_SPECS_ENABLED = CONFIG_PREFIX + ".onTheFlySpecs.enabled";
    public static final String ON_THE_FLY_SPECS_DELAY_BEFORE_RETRY_MS = CONFIG_PREFIX + ".onTheFlySpecs.delayBeforeRetryMs";
    public static final String API_SECRETS_ALLOW_PROVIDERS_FROM_CONFIGURATION = CONFIG_PREFIX + ".allowProvidersFromConfiguration";
}
