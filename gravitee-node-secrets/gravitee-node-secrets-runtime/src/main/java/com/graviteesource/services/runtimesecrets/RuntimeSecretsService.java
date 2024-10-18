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

import com.graviteesource.services.runtimesecrets.renewal.RenewalService;
import com.graviteesource.services.runtimesecrets.spec.SpecRegistry;
import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.secrets.runtime.providers.SecretProviderDeployer;
import io.gravitee.node.api.secrets.runtime.spec.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.Duration;
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
    private final RenewalService renewalService;
    private final Environment environment;

    @Override
    protected void doStart() throws Exception {
        secretProviderDeployer.init();
        renewalService.start();
        startDemo();
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

    public <T> void onDefinitionUnDeploy(String envId, @Nonnull T definition, @Nullable Map<String, String> metadata) {
        processor.onDefinitionUnDeploy(envId, definition, metadata);
    }

    private void startDemo() {
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
        specLifecycleService.deploy(
            new Spec(
                "49a538ff-ac6e-4534-b3d1-ab37519569e4",
                "renewable-api-keys",
                "/mock/rotating",
                "api-key-1",
                null,
                true,
                false,
                new Resolution(Resolution.Type.POLL, Duration.ofSeconds(5)),
                acls("transform-headers"),
                "DEFAULT"
            )
        );
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
        String fieldToAdd = properties.getProperty("updateSpecACLAddField", "");
        String valueKind = properties.getProperty("updateSpecACLSetKind", "");
        String specToUndeploy = properties.getProperty("undeploySpec", "");
        String otfSpecWithACL = properties.getProperty("otfSpecWithACL", "");
        int otfSpecWithRenewal = Integer.parseInt(properties.getProperty("otfSpecWithRenewal", "0"));

        if (!otfSpecWithACL.isEmpty()) {
            specLifecycleService.deploy(
                new Spec(
                    "f9024ec8-ad20-4834-8962-9c9153218983",
                    "case-1-api-key",
                    "/mock/case1",
                    "api-key",
                    null,
                    false,
                    false,
                    otfSpecWithRenewal > 0 ? new Resolution(Resolution.Type.POLL, Duration.ofSeconds(otfSpecWithRenewal)) : null,
                    acls(otfSpecWithACL),
                    "DEFAULT"
                )
            );
        }
        if (!pluginToAdd.equals("ignore")) {
            deployStaticApiKey(pluginToAdd, valueKind, fieldToAdd);
        }

        if (!specToUndeploy.isEmpty()) {
            Spec spec = specRegistry.getFromName("DEFAULT", specToUndeploy);
            if (spec != null) {
                specLifecycleService.undeploy(spec);
            }
        }
    }

    private void deployStaticApiKey(String pluginToAdd, String valueKind, String fieldToAdd) {
        specLifecycleService.deploy(
            new Spec(
                "e69328d2-cdb0-4970-a94e-c521ff03f1d5",
                "case2-api-key",
                "/mock/case2",
                "api-key",
                null,
                false,
                false,
                null,
                acls(pluginToAdd, valueKind, fieldToAdd),
                "DEFAULT"
            )
        );
    }

    private ACLs acls(String pluginToAdd) {
        return acls(pluginToAdd, "", "");
    }

    private ACLs acls(String pluginToAdd, String valueKind, String fieldToAdd) {
        if (pluginToAdd.isEmpty()) {
            return null;
        }
        return new ACLs(
            valueKind.isEmpty() ? null : ValueKind.valueOf(valueKind.toUpperCase()),
            null,
            List.of(new ACLs.PluginACL(pluginToAdd, fieldToAdd.isEmpty() ? null : List.of(fieldToAdd)))
        );
    }
}
