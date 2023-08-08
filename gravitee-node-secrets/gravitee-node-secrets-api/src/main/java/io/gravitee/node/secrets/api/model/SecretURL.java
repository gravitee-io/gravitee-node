package io.gravitee.node.secrets.api.model;

import com.google.common.base.Splitter;
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
public record SecretURL(String provider, String path, String key, Multimap<String, String> query) {
    public static final char URL_SEPARATOR = '/';
    private static final Splitter urlPathSplitter = Splitter.on(URL_SEPARATOR);
    private static final Splitter queryParamSplitter = Splitter.on('&');
    private static final Splitter queryParamKeyValueSplitter = Splitter.on('=');
    private static final Splitter keyMapParamValueSplitter = Splitter.on(':');
    public static final String SCHEME = "secret://";

    public static SecretURL from(String url) {
        url = Objects.requireNonNull(url).trim();
        if (!url.startsWith(SCHEME)) {
            throwFormatError(url);
        }
        String schemeLess = url.substring(SCHEME.length());
        int firstSlash = schemeLess.indexOf('/');
        if (firstSlash < 0 || firstSlash == schemeLess.length() - 1) {
            throwFormatError(url);
        }

        String provider = schemeLess.substring(0, firstSlash).trim();
        int questionMarkPos = schemeLess.indexOf('?');
        if (questionMarkPos == firstSlash + 1) {
            throwFormatError(url);
        }

        String path;
        final String key;
        final Multimap<String, String> query;

        if (questionMarkPos > 0) {
            path = schemeLess.substring(provider.length() + 1, questionMarkPos).trim();
            query = parseQuery(schemeLess.substring(questionMarkPos + 1));
        } else {
            path = schemeLess.substring(provider.length() + 1).trim();
            query = MultimapBuilder.hashKeys().arrayListValues().build();
        }

        int columnIndex = path.lastIndexOf(':');
        if (columnIndex > path.lastIndexOf(URL_SEPARATOR)) {
            key = path.substring(columnIndex + 1);
            path = path.substring(0, columnIndex);
        } else {
            key = null;
        }

        // remove trailing slashes
        while (!path.isEmpty() && path.charAt(path.length() - 1) == URL_SEPARATOR) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.isBlank()) {
            throwFormatError(url);
        }

        if (urlPathSplitter.splitToList(path).stream().anyMatch(String::isBlank)) {
            throwFormatError(url);
        }

        return new SecretURL(provider, path, key, query);
    }

    private static void throwFormatError(String url) {
        throw new IllegalArgumentException(
            "Secret URL '%s' should have the following format %s<provider>/<path or name>[:<key>][?option=value1&option=value2]".formatted(
                    url,
                    SCHEME
                )
        );
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
