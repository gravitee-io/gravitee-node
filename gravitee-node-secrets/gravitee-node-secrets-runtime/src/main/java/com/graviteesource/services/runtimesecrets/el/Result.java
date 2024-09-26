package com.graviteesource.services.runtimesecrets.el;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Result(Type type, String value) {
    enum Type {
        VALUE,
        EMPTY,
        NOT_FOUND,
        KEY_NOT_FOUND,
        DENIED,
        ERROR,
    }
}
