package com.graviteesource.services.runtimesecrets.config;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Config(boolean allowOnTheFlySpecs, boolean allowEmptyACLSpecs) {
    public static final String ALLOW_EMPTY_ACL_SPECS = "api.secrets.allowEmptyNoACLsSpecs";
    public static final String ALLOW_ON_THE_FLY_SPECS = "api.secrets.allowOnTheFlySpecs";
}
