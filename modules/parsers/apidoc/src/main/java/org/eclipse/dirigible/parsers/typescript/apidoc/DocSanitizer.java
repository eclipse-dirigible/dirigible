/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.typescript.apidoc;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocSanitizer {

    /**
     * Allowed HTML tags that won't be escaped.
     */
    private static final Set<String> SAFE_TAGS = Set.of("b", "i", "em", "strong", "code", "pre", "br");

    private static final Pattern TAG_PATTERN = Pattern.compile("</?([a-zA-Z0-9]+)(\\s[^>]*)?>");

    public static String sanitize(String doc) {
        if (doc == null || doc.isEmpty()) {
            return doc;
        }

        StringBuilder result = new StringBuilder();
        int last = 0;
        Matcher matcher = TAG_PATTERN.matcher(doc);

        while (matcher.find()) {
            String tag = matcher.group(0);
            String name = matcher.group(1)
                                 .toLowerCase();

            // Copy text before this tag
            result.append(doc, last, matcher.start());

            if (SAFE_TAGS.contains(name)) {
                // Keep safe tags intact
                result.append(tag);
            } else {
                // Escape unsafe tags to avoid Vue parser errors
                result.append(tag.replace("<", "&lt;")
                                 .replace(">", "&gt;"));
            }

            last = matcher.end();
        }

        // Append the rest
        result.append(doc.substring(last));

        // Fix unclosed <p> and broken block tags
        return autoCloseTags(result.toString()).replace("\n", "<br/>");
    }

    private static String autoCloseTags(String input) {
        // Close any <p> without </p>
        int opens = count(input, "<p>");
        int closes = count(input, "</p>");
        while (closes < opens) {
            input += "</p>";
            closes++;
        }
        return input;
    }

    private static int count(String text, String token) {
        int idx = 0, count = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            idx += token.length();
            count++;
        }
        return count;
    }
}
