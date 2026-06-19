/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import java.util.Map;
import java.util.Set;

/**
 * Translates an {@code IntegrationIntent}'s {@code method}/{@code url} into the Java the generated
 * {@code @Listener} pastes in. Pure (no Spring/IO) so the HTTP-method mapping and the
 * {@code @config:} URL sugar are unit-tested directly.
 */
public final class IntegrationSupport {

    /** HTTP method (author token, upper-case) -&gt; {@code sdk.http.HttpClient} method. */
    static final Map<String, String> METHODS = Map.of("GET", "get", "POST", "post", "PUT", "put", "PATCH", "patch", "DELETE", "delete");

    /** Methods that carry a request body (the forwarded entity JSON). */
    static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");

    private static final String CONFIG_PREFIX = "@config:";

    private IntegrationSupport() {}

    /**
     * @param method the author-facing HTTP method (case-insensitive), may be {@code null} (defaults
     *        POST)
     * @return whether it maps to a supported {@code HttpClient} method
     */
    public static boolean isSupportedMethod(String method) {
        return METHODS.containsKey(normalize(method));
    }

    /**
     * @param method the author-facing HTTP method
     * @return the {@code sdk.http.HttpClient} method name ({@code post}/{@code get}/...)
     */
    public static String clientMethod(String method) {
        return METHODS.get(normalize(method));
    }

    /**
     * @param method the author-facing HTTP method
     * @return whether the request carries the forwarded entity JSON as its body
     */
    public static boolean hasBody(String method) {
        return METHODS_WITH_BODY.contains(normalize(method));
    }

    /**
     * The URL as a Java {@code String} expression: an {@code @config:KEY} reference becomes a
     * {@code Configurations.get("KEY")} lookup (resolved server-side), otherwise a quoted literal.
     *
     * @param url the authored URL
     * @return a Java {@code String} expression
     */
    public static String urlExpression(String url) {
        if (url == null) {
            return "null";
        }
        String trimmed = url.trim();
        if (trimmed.startsWith(CONFIG_PREFIX)) {
            return "Configurations.get(\"" + trimmed.substring(CONFIG_PREFIX.length())
                                                    .trim()
                    + "\")";
        }
        return "\"" + trimmed.replace("\\", "\\\\")
                             .replace("\"", "\\\"")
                + "\"";
    }

    private static String normalize(String method) {
        return method == null ? "POST"
                : method.trim()
                        .toUpperCase();
    }
}
