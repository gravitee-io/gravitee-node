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
package com.graviteesource.services.runtimesecrets;

import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.runtime.spec.ACLs;
import io.gravitee.node.api.secrets.runtime.spec.Spec;
import io.gravitee.node.api.secrets.runtime.spec.SpecLifecycleService;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class RuntimeSecretsService extends AbstractService<RuntimeSecretsService> {

    private final Processor processor;
    private final SpecLifecycleService specLifecycleService;
    private final SpecRegistry specRegistry;
    private final SecretProviderDeployer secretProviderDeployer;
    private final Environment environment;

    @Override
    protected void doStart() throws Exception {
        secretProviderDeployer.init();
        startWatch(environment.getProperty("rtsecdemodir"));
        specLifecycleService.deploy(
            new Spec(
                "a9c0ea5b-aac8-4064-bed5-47021082f8a2",
                "dyn-api-keys",
                "/mock/dynamic-key/named/apikeys",
                null,
                null,
                true,
                false,
                null,
                acls("transform-headers"),
                "DEFAULT"
            )
        );
    }

    public void deploy(Spec spec) {
        specLifecycleService.deploy(spec);
    }

    public void undeploy(Spec spec) {
        specLifecycleService.undeploy(spec);
    }

    public <T> void onDefinitionDeploy(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        processor.onDefinitionDeploy(envId, definition, metadata);
    }

    private void startWatch(String directory) {
        if (directory == null) {
            return;
        }
        Path watched = Path.of(directory);
        if (!Files.exists(watched)) {
            return;
        }

        final WatchService watcherService;
        try {
            watcherService = FileSystems.getDefault().newWatchService();
            watched.register(watcherService, StandardWatchEventKinds.ENTRY_MODIFY);
            Schedulers
                .newThread()
                .scheduleDirect(() -> {
                    while (true) {
                        WatchKey watchKey = watcherService.poll();
                        if (watchKey == null) {
                            continue;
                        }
                        final Optional<Path> path = watchKey
                            .pollEvents()
                            .stream()
                            .map(watchEvent -> ((WatchEvent<Path>) watchEvent).context())
                            .filter(file -> file.toString().endsWith(".properties"))
                            .findFirst();

                        if (path.isPresent()) {
                            Properties properties = new Properties();
                            try {
                                properties.load(new FileReader(watched.resolve(path.get()).toFile()));
                                handleDemo(properties);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                        if (!watchKey.reset()) {
                            break;
                        }
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void handleDemo(Properties properties) {
        String pluginToAdd = properties.getProperty("updateSpecACLAddPlugin", "ignore");
        String specToUndeploy = properties.getProperty("undeploySpec", "");
        String addSpec = properties.getProperty("newSpecWithACL", "");

        if (!addSpec.isEmpty()) {
            specLifecycleService.deploy(
                new Spec(
                    "f9024ec8-ad20-4834-8962-9c9153218983",
                    null,
                    "/mock/static/uri",
                    "api-key",
                    null,
                    false,
                    false,
                    null,
                    acls(addSpec),
                    "DEFAULT"
                )
            );
        }
        if (!pluginToAdd.equals("ignore")) {
            deployStaticApiKey(pluginToAdd);
        }

        if (!specToUndeploy.isEmpty()) {
            Spec spec = specRegistry.getFromName("DEFAULT", specToUndeploy);
            specLifecycleService.undeploy(spec);
        }
    }

    private void deployStaticApiKey(String pluginToAdd) {
        specLifecycleService.deploy(
            new Spec(
                "e69328d2-cdb0-4970-a94e-c521ff03f1d5",
                "static-api-key",
                "/mock/static/named",
                "api-key",
                null,
                false,
                false,
                null,
                acls(pluginToAdd),
                "DEFAULT"
            )
        );
    }

    private ACLs acls(String pluginToAdd) {
        if (pluginToAdd.isEmpty()) {
            return null;
        }
        return new ACLs(null, List.of(new ACLs.PluginACL(pluginToAdd, null)));
    }
}
