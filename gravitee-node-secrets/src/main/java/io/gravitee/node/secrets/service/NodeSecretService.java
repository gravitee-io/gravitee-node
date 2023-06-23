/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.secrets.service;

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.secrets.*;
import io.gravitee.node.secrets.model.Secret;
import io.gravitee.node.secrets.model.SecretAccessContext;
import io.gravitee.node.secrets.model.SecretEvent;
import io.gravitee.node.secrets.plugin.SecretProvidersManager;
import io.reactivex.rxjava3.core.Flowable;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NodeSecretService extends AbstractService<NodeSecretService> implements SecretProvider {

    // TODO register this where it should
    // TODO how will I be able to use this (configured) service to resolve secrets in the configuration
    // New kind of config ?
    // New service that will load the config once, to extract secret provider information ?

    private final SecretProvidersManager secretProvidersManager;
    private final Configuration configuration;
    private final SecretAccessService secretAccessService;

    public NodeSecretService(
        SecretProvidersManager secretProvidersManager,
        SecretAccessService secretAccessService,
        Configuration configuration
    ) {
        this.secretProvidersManager = secretProvidersManager;
        this.secretAccessService = secretAccessService;
        this.configuration = configuration;
    }

    @Override
    protected void doStart() throws Exception {
        // read configuration

        // call matching factory for each configured providers

        // register

        // call doStart on provider
    }

    @Override
    protected void doStop() throws Exception {
        // find all secret provider

        // call doStop
    }

    @Override
    public Secret resolve(SecretAccessContext context, String secretName) throws SecretAccessDeniedException {
        // find secret provider

        // call resolve

        return null;
    }

    @Override
    public Flowable<SecretEvent> watch(SecretAccessContext context, String secretName, SecretEvent.Type... events)
        throws SecretAccessDeniedException {
        return null;
    }
}
