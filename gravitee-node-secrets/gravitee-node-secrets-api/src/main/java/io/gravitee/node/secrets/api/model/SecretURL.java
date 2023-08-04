package io.gravitee.node.secrets.api.model;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record SecretURL(String provider, String path, Multimap<String, String> query) {
    public static final char URL_SEPARATOR = '/';
    private static final Splitter urlPathSplitter = Splitter.on(URL_SEPARATOR);
    private static final Splitter queryParamSplitter = Splitter.on('&');
    private static final Splitter queryParamKeyValueSplitter = Splitter.on('=');
    private static final Splitter keyMapParamValueSplitter = Splitter.on(':');
    public static final String SCHEME = "secret://";
    public static final String URL_FORMAT_ERROR =
        "Secret URL '%s' should have the following format %s<provider>/<word>[/<word>]*[?key1=value1&key2=value2]";

    public static SecretURL from(String url) {
        url = Objects.requireNonNull(url).trim();
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
        final Multimap<String, String> query;

        if (questionMarkPos > 0) {
            path = schemeLess.substring(provider.length(), questionMarkPos);
            query = parseQuery(schemeLess.substring(questionMarkPos + 1));
        } else {
            path = schemeLess.substring(provider.length());
            query = MultimapBuilder.hashKeys().arrayListValues().build();
        }

        SecretURL secretURL = new SecretURL(provider, path, query);
        if (secretURL.pathAsList().stream().map(String::trim).anyMatch(Strings::isNullOrEmpty)) {
            throw new IllegalArgumentException("Secret URL '%s' contains spaces-only url parts".formatted(url));
        }

        return secretURL;
    }

    private static Multimap<String, String> parseQuery(String substring) {
        Multimap<String, String> query = MultimapBuilder.hashKeys().arrayListValues().build();
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
            .entries()
            .stream()
            .anyMatch(e -> Objects.equals(e.getKey(), WellKnownQueryParam.WATCH) && Boolean.parseBoolean(e.getValue()));
    }

    public Map<String, SecretMap.WellKnownSecretKey> wellKnowKeyMap() {
        record Mapping(String secretKey, SecretMap.WellKnownSecretKey wellKnow) {}
        return query()
            .get(WellKnownQueryParam.KEYMAP)
            .stream()
            .map(keyMap -> {
                List<String> mapping = keyMapParamValueSplitter.splitToList(keyMap);
                if (mapping.size() == 2) {
                    // eg. certificate:tls.crt
                    String wellKnown = mapping.get(0).trim().toUpperCase();
                    String secretKey = mapping.get(1).trim();
                    if (wellKnown.isEmpty() || secretKey.isEmpty()) {
                        throw new IllegalArgumentException("keymap '%s' is not valid".formatted(keyMap));
                    }
                    try {
                        return new Mapping(secretKey, SecretMap.WellKnownSecretKey.valueOf(wellKnown));
                    } catch (IllegalArgumentException e) {
                        // no op, will return "empty"
                    }
                }
                return new Mapping(null, null);
            })
            .filter(mapping -> mapping.wellKnow() != null)
            .collect(Collectors.toMap(Mapping::secretKey, Mapping::wellKnow));
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WellKnownQueryParam {

        public static final String WATCH = "watch";
        public static final String KEYMAP = "keymap";
        public static final String NAMESPACE = "namespace";
    }
}
