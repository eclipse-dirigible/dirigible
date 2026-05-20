/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles route path templates like {@code "/items/{id}"} into anchored regex patterns with named
 * capture groups for each {@code {placeholder}}.
 *
 * <p>
 * Empty / blank templates compile to a pattern that matches the empty string or a single trailing
 * slash — i.e. the controller's base URL itself.
 */
final class PathPattern {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)\\}");

    private PathPattern() {}

    /** Result holder: the compiled pattern plus the ordered list of placeholder names. */
    record Compiled(Pattern pattern, List<String> placeholders) {
    }

    static Compiled compile(String template) {
        String t = template == null ? "" : template.trim();
        if (t.isEmpty() || "/".equals(t)) {
            return new Compiled(Pattern.compile("/?"), Collections.emptyList());
        }
        String normalized = t.startsWith("/") ? t : "/" + t;
        Matcher m = PLACEHOLDER.matcher(normalized);
        StringBuilder regex = new StringBuilder();
        List<String> names = new ArrayList<>();
        int prev = 0;
        while (m.find()) {
            regex.append(Pattern.quote(normalized.substring(prev, m.start())));
            String name = m.group(1);
            if (names.contains(name)) {
                throw new IllegalArgumentException("Duplicate path placeholder [" + name + "] in [" + template + "]");
            }
            names.add(name);
            regex.append("(?<")
                 .append(name)
                 .append(">[^/]+)");
            prev = m.end();
        }
        if (prev < normalized.length()) {
            regex.append(Pattern.quote(normalized.substring(prev)));
        }
        regex.append("/?");
        return new Compiled(Pattern.compile(regex.toString()), Collections.unmodifiableList(names));
    }

    /**
     * Specificity score for ordering routes when more than one could match. Lower scores are more
     * specific (preferred first). Within the same number of placeholders the longer template wins.
     */
    static int specificity(String template, int placeholderCount) {
        int len = template == null ? 0 : template.length();
        return (placeholderCount * 10_000) - len;
    }
}
