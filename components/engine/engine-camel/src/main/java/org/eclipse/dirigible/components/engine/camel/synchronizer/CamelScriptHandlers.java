/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.synchronizer;

import java.util.Locale;
import java.util.Optional;

import org.eclipse.dirigible.components.engine.camel.invoke.Invoker;

/**
 * Finds the script handler a route runs, which only the JavaScript engine can run (#8174). A route
 * reaches a script through the {@code dirigible-java-script} endpoint or through the
 * {@link Invoker} bean, which runs the script named by the exchange's {@code resource} property.
 */
final class CamelScriptHandlers {

    private static final String SCRIPT_SCHEME = "dirigible-java-script";

    private static final String INVOKER_CLASS = Invoker.class.getName();

    private static final String[] SCRIPT_EXTENSIONS = {".ts", ".js", ".mjs", ".cjs"};

    /** What separates the tokens of a route in either of its YAML and XML shapes. */
    private static final String TOKEN_SEPARATORS = "\\s\"'<>=:,(){}\\[\\]";

    private CamelScriptHandlers() {}

    /**
     * The script the route runs, when it runs one.
     *
     * @param location the location of the route
     * @param content the route definition
     * @return the first project path of a script the route names - else the first script file name,
     *         else the route's own location; empty when it runs no script
     */
    static Optional<String> find(String location, String content) {
        if (!content.contains(SCRIPT_SCHEME) && !content.contains(INVOKER_CLASS)) {
            return Optional.empty();
        }
        // A handler is named by its project path; a bare file name is prose, such as a step's description.
        String bareName = null;
        for (String token : content.split("[" + TOKEN_SEPARATORS + "]+")) {
            if (isScriptPath(token)) {
                if (token.contains("/")) {
                    return Optional.of(token);
                }
                if (bareName == null) {
                    bareName = token;
                }
            }
        }
        return Optional.of(bareName != null ? bareName : location);
    }

    private static boolean isScriptPath(String token) {
        String lowerCase = token.toLowerCase(Locale.ROOT);
        for (String extension : SCRIPT_EXTENSIONS) {
            if (lowerCase.length() > extension.length() && lowerCase.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
