package io.gravitee.node.secrets.api.model;

import com.google.common.base.Splitter;

import java.util.*;

import com.google.common.base.Strings;
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
    public static final String URL_FORMAT_ERROR = "Secret URL '%s' should have the following format %s<provider>/<word>[/<word>]*[?key=value&key2=value2]";

    public static SecretURL from(String url) {
        if (!url.startsWith(SCHEME)) {
            throw new IllegalArgumentException(URL_FORMAT_ERROR.formatted(url, SCHEME));
        }
        String schemeLess = url.substring(SCHEME.length());
        int firstSlash = schemeLess.indexOf('/');
        if (firstSlash < 0 || firstSlash == schemeLess.length() - 1) {
            throw new IllegalArgumentException(URL_FORMAT_ERROR.formatted(url, SCHEME));
        }

        String provider = schemeLess.substring(0, firstSlash);
        int questionMarkPos = schemeLess.indexOf('?');
        if (questionMarkPos == firstSlash + 1) {
            throw new IllegalArgumentException(URL_FORMAT_ERROR.formatted(url, SCHEME));
        }

        final String path;
        final Map<String, String> query;

        if (questionMarkPos > 0) {
            path = schemeLess.substring(provider.length(), questionMarkPos);
            query = parseQuery(schemeLess.substring(questionMarkPos + 1));
        } else {
            path = schemeLess.substring(provider.length());
            query = Map.of();
        }

        SecretURL secretURL = new SecretURL(provider, path, query);
        if (secretURL.pathAsList().stream().map(String::trim).anyMatch(Strings::isNullOrEmpty)) {
            throw new IllegalArgumentException("Secret URL '%s' contains spaces-only url parts".formatted(url));
        }

        return secretURL;
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
                .anyMatch(e -> Objects.equals(e.getKey(), WellKnownQueryParam.WATCH) && Boolean.parseBoolean(e.getValue()));
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WellKnownQueryParam {

        public static final String WATCH = "watch";
    }
}
