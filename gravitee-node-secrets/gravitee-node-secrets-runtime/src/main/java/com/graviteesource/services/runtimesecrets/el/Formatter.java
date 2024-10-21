/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.graviteesource.services.runtimesecrets.el;

import static io.gravitee.common.secrets.RuntimeContext.EL_VARIABLE;

import io.gravitee.node.api.secrets.runtime.discovery.Definition;
import io.gravitee.node.api.secrets.runtime.discovery.DiscoveryContext;
import io.gravitee.node.api.secrets.runtime.discovery.PayloadLocation;
import io.gravitee.node.api.secrets.runtime.discovery.Ref;
import java.util.UUID;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Formatter {

    public static final String FROM_GRANT_TEMPLATE = "{#secrets.fromGrant('%s', " + "#" + EL_VARIABLE + ")}";
    public static final String FROM_GRANT_EL_KEY_TEMPLATE = "{#secrets.fromGrant('%s', %s, " + "#" + EL_VARIABLE + ")}";
    public static final String METHOD_NAME_SUFFIX = "WithName";
    public static final String METHOD_URI_SUFFIX = "WithUri";
    public static final String FROM_GRANT_WITH_TEMPLATE = "{#secrets.fromGrant%s('%s', '%s', '%s', %s)}";
    public static final String FROM_EL_WITH_TEMPLATE = "{#secrets.fromEL%s('%s', %s, '%s', '%s'%s)}";

    public static String computeELFromStatic(DiscoveryContext context, String envId) {
        if (context.ref().mainExpression().isEL()) {
            throw new IllegalArgumentException("mis-usage this method only supports main secret expression as a literal  string");
        }
        final String mainSpec = context.ref().mainExpression().value();
        String el;
        if (context.ref().secondaryType() != null) {
            switch (context.ref().secondaryType()) {
                case KEY -> {
                    if (context.ref().secondaryExpression().isLiteral()) {
                        el = fromGrant(context.id());
                    } else {
                        el = fromGrant(context.id(), context.ref().secondaryExpression().value());
                    }
                }
                case NAME -> el =
                    fromGrantWithTemplate(METHOD_NAME_SUFFIX, context.id(), envId, mainSpec, context.ref().secondaryExpression());
                case URI -> el =
                    fromGrantWithTemplate(METHOD_URI_SUFFIX, context.id(), envId, mainSpec, context.ref().secondaryExpression());
                default -> {
                    throw new IllegalArgumentException("secondary type unknown: %s".formatted(context.ref().secondaryType()));
                }
            }
        } else {
            el = fromGrant(context.id());
        }
        return el;
    }

    public static String computeELFromEL(DiscoveryContext context, String envId, Definition... others) {
        if (context.ref().mainExpression().isLiteral()) {
            throw new IllegalArgumentException("mis-usage this method only supports main secret expression as an EL");
        }
        String el;
        switch (context.ref().mainType()) {
            case NAME -> el =
                FROM_EL_WITH_TEMPLATE.formatted(
                    METHOD_NAME_SUFFIX,
                    envId,
                    context.ref().mainExpression().value(),
                    context.location().definition().kind(),
                    context.location().definition().id(),
                    others(context.location().payloadLocations())
                );
            case URI -> el =
                FROM_EL_WITH_TEMPLATE.formatted(
                    METHOD_URI_SUFFIX,
                    envId,
                    context.ref().mainExpression().value(),
                    context.location().definition().kind(),
                    context.location().definition().id(),
                    others(context.location().payloadLocations())
                );
            default -> {
                throw new IllegalArgumentException("main type unknown: %s".formatted(context.ref().secondaryType()));
            }
        }
        return el;
    }

    private static String others(PayloadLocation... ignoredTODO) {
        return "";
    }

    private static String fromGrantWithTemplate(
        String methodSuffix,
        UUID id,
        String envId,
        String literalExpression,
        Ref.Expression secondaryExpression
    ) {
        return FROM_GRANT_WITH_TEMPLATE.formatted(methodSuffix, id, envId, literalExpression, quoteLiteral(secondaryExpression));
    }

    private static String fromGrant(UUID id) {
        return FROM_GRANT_TEMPLATE.formatted(id);
    }

    private static String fromGrant(UUID id, String key) {
        return FROM_GRANT_EL_KEY_TEMPLATE.formatted(id, key);
    }

    private static String quoteLiteral(Ref.Expression expression) {
        // literal string or EL (as is)
        return expression.isLiteral() ? "'%s'".formatted(expression.value()) : expression.value();
    }
}
