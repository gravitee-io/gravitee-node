package io.gravitee.node.api.secrets.runtime.discovery;

public record PayloadLocation(String kind, String id) {
    public static final String PLUGIN_KIND = "plugin";
    public static final PayloadLocation NOWHERE = new PayloadLocation("", "");
}
