package io.gravitee.node.secrets.api.model;

import com.google.common.base.Splitter;
import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretURL(String provider, String path, Map<String, String> query) {
    private static final Splitter urlPathSplitter = Splitter.on('/');
    private static final Splitter queryParamSplitter = Splitter.on('&');
    private static final Splitter queryParamKeyValueSplitter = Splitter.on('=');
    public static final String SCHEME = "secret://";

    public static SecretURL from(String location) {
        if (!location.startsWith(SCHEME)) {
            throw new IllegalArgumentException("secret URL should start with %s".formatted(SCHEME));
        }
        String schemeLess = location.substring(SCHEME.length());
        int firstSlash = schemeLess.indexOf('/');
        if (firstSlash < 0) {
            throw new IllegalArgumentException("secret URL should start with %s%s/".formatted(SCHEME, "<provider>"));
        }
        String provider = schemeLess.substring(0, firstSlash);
        int interrogationMarkIndex = location.indexOf('?');
        final String path;
        final Map<String, String> query;
        if (interrogationMarkIndex > 0) {
            path = schemeLess.substring(provider.length(), interrogationMarkIndex);
            query = parseQuery(schemeLess.substring(interrogationMarkIndex + 1));
        } else {
            path = schemeLess.substring(provider.length());
            query = Map.of();
        }

        return new SecretURL(provider, path, query);
    }

    private static Map<String, String> parseQuery(String substring) {
        Map<String, String> query = new HashMap<>();
        queryParamSplitter
            .split(substring)
            .forEach(pair -> {
                Iterable<String> parts = queryParamKeyValueSplitter.split(pair);
                Iterator<String> iterator = parts.iterator();
                if (iterator.hasNext()) {
                    String key = iterator.next();
                    if (iterator.hasNext()) {
                        query.put(key, iterator.next());
                    } else {
                        query.put(key, "true");
                    }
                }
            });
        return query;
    }

    public List<String> pathAsList() {
        String noTrailingSlashedPath = path;
        while (noTrailingSlashedPath.endsWith("/")) {
            noTrailingSlashedPath = noTrailingSlashedPath.substring(0, noTrailingSlashedPath.length() - 1);
        }
        // elude starting slash
        return urlPathSplitter.splitToList(noTrailingSlashedPath.substring(1));
    }

    public boolean isWatchable() {
        return query()
            .entrySet()
            .stream()
            .anyMatch(e -> Objects.equals(e.getKey(), WellKnownQueryParam.WATCH) && Objects.equals(e.getValue(), "true"));
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WellKnownQueryParam {

        public static final String WATCH = "watch";
    }
}
