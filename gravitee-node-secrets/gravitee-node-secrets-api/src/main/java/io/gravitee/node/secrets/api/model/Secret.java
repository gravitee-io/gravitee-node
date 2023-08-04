package io.gravitee.node.secrets.api.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@Accessors(fluent = true)
@EqualsAndHashCode
public final class Secret {

    private final Object data;
    private final boolean base64Encoded;

    public Secret(Object data) {
        this(data, false);
    }

    public Secret(Object data, boolean base64Encoded) {
        if (!(data instanceof String) && !(data instanceof byte[])) {
            throw new IllegalArgumentException("secret can only be of type String or byte[] any may not be null");
        }
        this.data = data;
        this.base64Encoded = base64Encoded;
    }

    public boolean isEmpty() {
        if (data instanceof String str) {
            return str.isEmpty();
        } else if (data instanceof byte[] buf) {
            return buf.length == 0;
        }
        return true;
    }

    public byte[] asBytes() {
        if (data instanceof String str) {
            return base64Encoded ? Base64.getDecoder().decode(str) : str.getBytes(StandardCharsets.UTF_8);
        }
        if (data instanceof byte[] buf) {
            return base64Encoded ? Base64.getDecoder().decode(buf) : buf;
        }
        return new byte[0];
    }

    public String asString() {
        if (data instanceof String str) {
            return base64Encoded ? new String(Base64.getDecoder().decode(str)) : str;
        }
        if (data instanceof byte[] buf) {
            return base64Encoded ? new String(Base64.getDecoder().decode(buf)) : new String(buf);
        }
        return "";
    }
}
