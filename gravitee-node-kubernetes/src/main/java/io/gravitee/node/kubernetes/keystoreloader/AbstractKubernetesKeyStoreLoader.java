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
import io.gravitee.node.api.certificate.KeyStoreEvent;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.certificates.AbstractKeyStoreLoader;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractKubernetesKeyStoreLoader<T> extends AbstractKeyStoreLoader<KeyStoreLoaderOptions> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractKubernetesKeyStoreLoader.class);
    protected static final int RETRY_DELAY_MILLIS = 10000;

    protected final KubernetesClient kubernetesClient;
    protected final Map<String, KeyStore> keyStoresByLocation;
    protected final Map<String, ResourceQuery<T>> resources = new HashMap<>();
    private Disposable disposable;

    protected AbstractKubernetesKeyStoreLoader(KeyStoreLoaderOptions options, KubernetesClient kubernetesClient) {
        super(options);
        this.kubernetesClient = kubernetesClient;
        this.keyStoresByLocation = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
        try {
            init()
                .doOnComplete(() -> {
                    if (options.isWatch()) {
                        startWatch();
                    }
                })
                .blockingAwait();
        } catch (Exception throwable) {
            throw new IllegalArgumentException("An error occurred when trying to init certificates.", throwable);
        }
    }

    protected void startWatch() {
        this.disposable =
            watch()
                .observeOn(Schedulers.computation())
                .flatMapCompletable(t -> loadKeyStore(t).andThen(Completable.fromRunnable(this::emitKeyStoreEvent)))
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

    protected void emitKeyStoreEvent() {
        final KeyStore keyStore = KeyStoreUtils.merge(new ArrayList<>(keyStoresByLocation.values()), getPassword());
        String loaderId = id();
        this.onEvent(new KeyStoreEvent.LoadEvent(loaderId, keyStore, getPassword()));
    }
}
