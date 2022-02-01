/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.kubernetes.keystoreloader;

import io.gravitee.common.util.KeyStoreUtils;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.kubernetes.client.api.ResourceQuery;
import io.gravitee.node.api.certificate.KeyStoreBundle;
import io.gravitee.node.api.certificate.KeyStoreLoader;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractKubernetesKeyStoreLoader<T> implements KeyStoreLoader {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKubernetesKeyStoreLoader.class);
    protected static final int RETRY_DELAY_MILLIS = 10000;

    protected final KeyStoreLoaderOptions options;
    protected final KubernetesClient kubernetesClient;
    protected final List<Consumer<KeyStoreBundle>> listeners;
    protected final Map<String, KeyStore> keyStoresByLocation;
    protected final Map<String, ResourceQuery<T>> resources = new HashMap<>();
    protected KeyStoreBundle keyStoreBundle;

    private Disposable disposable;

    public AbstractKubernetesKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        this.options = options;
        this.kubernetesClient = kubernetesClient;
        this.listeners = new ArrayList<>();
        this.keyStoresByLocation = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        final Throwable throwable = init()
            .doOnComplete(() -> {
                if (options.isWatch()) {
                    startWatch();
                }
            })
            .blockingGet();

        if (throwable != null) {
            throw new IllegalArgumentException("An error occurred when trying to init certificates.", throwable);
        }
    }

    protected void startWatch() {
        this.disposable =
            watch()
                .observeOn(Schedulers.computation())
                .flatMapCompletable(t -> loadKeyStore(t).andThen(Completable.fromRunnable(this::refreshKeyStoreBundle)))
                .doOnError(throwable -> logger.error("An error occurred during keystore refresh. Restarting watch.", throwable))
                .retry()
                .subscribe();
    }

    @Override
    public void stop() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    protected abstract Flowable<T> watch();

    protected abstract Completable init();

    protected abstract Completable loadKeyStore(T elt);

    @Override
    public void addListener(Consumer<KeyStoreBundle> listener) {
        listeners.add(listener);
    }

    protected void refreshKeyStoreBundle() {
        final KeyStore keyStore = KeyStoreUtils.merge(new ArrayList<>(keyStoresByLocation.values()), options.getKeyStorePassword());
        this.keyStoreBundle = new KeyStoreBundle(keyStore, options.getKeyStorePassword(), options.getDefaultAlias());
        this.notifyListeners();
    }

    protected void notifyListeners() {
        listeners.forEach(consumer -> consumer.accept(keyStoreBundle));
    }
}
