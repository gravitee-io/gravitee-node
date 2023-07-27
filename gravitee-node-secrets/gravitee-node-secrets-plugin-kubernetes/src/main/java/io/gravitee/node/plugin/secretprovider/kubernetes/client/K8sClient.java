package io.gravitee.node.plugin.secretprovider.kubernetes.client;

import com.google.gson.reflect.TypeToken;
import io.gravitee.node.plugin.secretprovider.kubernetes.config.K8sConfig;
import io.gravitee.node.plugin.secretprovider.kubernetes.config.K8sSecretLocation;
import io.gravitee.node.secrets.api.errors.SecretManagerConfigurationException;
import io.gravitee.node.secrets.api.errors.SecretManagerException;
import io.gravitee.node.secrets.api.model.SecretEvent;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Watch;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class K8sClient {

    private final CoreV1Api api;
    private final Map<WatchKey, Flowable<K8sSecretWatchResult>> watches = new HashMap<>();

    public K8sClient(K8sConfig configuration) {
        ApiClient client;
        if (configuration.isClusterBased()) {
            try {
                client = ClientBuilder.cluster().build();
            } catch (IOException e) {
                throw new SecretManagerConfigurationException("could not locate kubernetes cluster", e);
            }
        } else {
            try (FileReader fileReader = new FileReader(configuration.getKubeConfigFile())) {
                client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(fileReader)).build();
            } catch (IOException e) {
                throw new SecretManagerConfigurationException("could not read kube configuration file", e);
            }
        }
        // infinite timeout
        OkHttpClient httpClient = client.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        client.setConnectTimeout(configuration.getTimeoutMs());
        client.setHttpClient(httpClient);
        this.api = new CoreV1Api();

        api.setApiClient(client);
    }

    public Optional<V1Secret> getSecret(K8sSecretLocation location) throws ApiException {
        final V1SecretList v1SecretList = api.listNamespacedSecret(
            location.namespace(),
            null,
            false,
            null,
            selector(location.secret()),
            null,
            1,
            null,
            null,
            null,
            false
        );
        if (v1SecretList.getItems().size() == 1) {
            return Optional.of(v1SecretList.getItems().get(0));
        } else {
            return Optional.empty();
        }
    }

    private static String selector(String secret) {
        return "metadata.name=%s".formatted(secret);
    }

    public Flowable<K8sSecretWatchResult> watchSecret(String namespace, String secret) {
        return watches.computeIfAbsent(
            new WatchKey(namespace, secret),
            key -> {
                PublishSubject<K8sSecretWatchResult> callbackErrors = PublishSubject.create();
                ApiCallbackAdapter callback = new ApiCallbackAdapter() {
                    @Override
                    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                        callbackErrors.onError(e);
                        callbackErrors.onComplete();
                    }
                };

                return Flowable.defer(() -> {
                    Watch<V1Secret> watch = Watch.createWatch(
                        api.getApiClient(),
                        api.listNamespacedSecretCall(
                            namespace,
                            null,
                            false,
                            null,
                            selector(secret),
                            null,
                            -1,
                            null,
                            null,
                            null,
                            true,
                            callback
                        ),
                        new TypeToken<Watch.Response<V1Secret>>() {}.getType()
                    );
                    return Flowable
                        .<K8sSecretWatchResult>create(
                            emitter -> {
                                while (watch.hasNext()) {
                                    if (emitter.isCancelled()) {
                                        return;
                                    }
                                    final Watch.Response<V1Secret> response = watch.next();
                                    switch (response.type) {
                                        case "ADDED" -> emitter.onNext(new K8sSecretWatchResult(SecretEvent.Type.CREATED, response.object));
                                        case "MODIFIED" -> emitter.onNext(
                                            new K8sSecretWatchResult(SecretEvent.Type.UPDATED, response.object)
                                        );
                                        case "DELETED" -> emitter.onNext(
                                            new K8sSecretWatchResult(SecretEvent.Type.DELETED, response.object)
                                        );
                                        case "ERROR" -> emitter.tryOnError(new SecretManagerException(response.status.getMessage()));
                                        default -> emitter.tryOnError(new IllegalStateException("Unexpected value: " + response.type));
                                    }
                                }
                                emitter.onComplete();
                            },
                            BackpressureStrategy.ERROR
                        )
                        .mergeWith(callbackErrors.toFlowable(BackpressureStrategy.ERROR))
                        .share()
                        .doFinally(() -> {
                            closeOrLogError(watch);
                            watches.remove(key);
                        });
                });
            }
        );
    }

    private static void closeOrLogError(Watch<V1Secret> watch) {
        try {
            watch.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private abstract static class ApiCallbackAdapter implements ApiCallback<V1Secret> {

        @Override
        public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
            // no op
        }

        @Override
        public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
            // no op
        }

        @Override
        public void onSuccess(V1Secret result, int statusCode, Map<String, List<String>> responseHeaders) {
            // no op
        }
    }

    public record WatchKey(String namespace, String secret) {}
}
