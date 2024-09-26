package io.gravitee.node.api.secrets.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * This represents a single secret value.
 * It can be constructed from a byte array or from a String. By default, data is NOT considered base64 encoded.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */

@Accessors(fluent = true)
@EqualsAndHashCode
public final class Secret {

    private final Object data;
    private final boolean base64Encoded;

    public Secret() {
        this("");
    }

    /**
     * Builds a secret value assuming it is not base64 encoded
     *
     * @param data a byte array or a String
     * @throws IllegalArgumentException if <code>data</code> is not byte array or String
     */
    public Secret(Object data) {
        this(data, false);
    }

    /**
     * Builds a secret value
     *
     * @param data          a byte array or a String
     * @param base64Encoded to declare data as base64 encoded
     * @throws IllegalArgumentException if <code>data</code> is not byte array or String or is null
     */
    public Secret(Object data, boolean base64Encoded) {
        if (!(data instanceof String) && !(data instanceof byte[])) {
            throw new IllegalArgumentException("secret can only be of type String or byte[] and must not be null");
        }
        this.data = data;
        this.base64Encoded = base64Encoded;
    }

    /**
     * Tests emptiness of the secret
     *
     * @return true is the secret contains empty string or byte array
     */
    public boolean isEmpty() {
        boolean result = true;
        if (data instanceof String str) {
            result = str.isEmpty();
        } else if (data instanceof byte[] buf) {
            result = buf.length == 0;
        }
        return result;
    }

    /**
     * If {@link Secret} is a String, convert to byte array assuming String is UTF-8.
     * If {@link Secret} is declared to contains base64 data, it will be decoded.
     *
     * @return secret value as bytes
     * @see Base64.Decoder#decode(byte[])
     */
    public byte[] asBytes() {
        byte[] result = new byte[0];
        if (data instanceof String str) {
            result = base64Encoded ? Base64.getDecoder().decode(str) : str.getBytes(StandardCharsets.UTF_8);
        }
        if (data instanceof byte[] buf) {
            result = base64Encoded ? Base64.getDecoder().decode(buf) : buf;
        }
        return result;
    }

    /**
     * If {@link Secret} is a byte array, convert to String assuming bytes are UTF-8.
     * If {@link Secret} is declared to contains base64 data, it will be decoded.
     *
     * @return secret value as String
     * @see Base64.Decoder#decode(byte[])
     */
    public String asString() {
        String result = "";
        if (data instanceof String str) {
            result = base64Encoded ? new String(Base64.getDecoder().decode(str)) : str;
        }
        if (data instanceof byte[] buf) {
            result = base64Encoded ? new String(Base64.getDecoder().decode(buf)) : new String(buf, StandardCharsets.UTF_8);
        }
        return result;
    }
}
