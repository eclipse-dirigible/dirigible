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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.components.intent.model.NotificationIntent;

/**
 * Pure translation helpers that turn a {@link NotificationIntent}'s author-facing fields into the
 * Java expressions the generated {@code @Listener} pastes in. Kept free of Spring/IO so the tricky
 * bits (recipient resolution, {@code {field}} interpolation, the {@code when} guard) are
 * unit-tested directly.
 *
 * <p>
 * v1 resolves <b>direct fields of the event entity</b> (rendered as {@code entity.<PascalField>})
 * and literals; relation-path values such as {@code member.email} are rejected by the parser and
 * are a later increment (they reuse the decision-resolver machinery to load the related entity).
 */
public final class NotificationSupport {

    private static final String[] EVENT_KINDS = {"onCreate", "onUpdate", "onDelete"};
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Pattern SIMPLE_COMPARISON = Pattern.compile("\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(==|!=)\\s*(.+?)\\s*");

    private NotificationSupport() {}

    /**
     * @param notification the notification
     * @return the lifecycle event kind it binds to
     *         ({@code onCreate}/{@code onUpdate}/{@code onDelete}), or {@code null}
     */
    public static String eventKind(NotificationIntent notification) {
        for (String kind : EVENT_KINDS) {
            if (notification.getEvent()
                            .get(kind) != null) {
                return kind;
            }
        }
        return null;
    }

    /**
     * @param notification the notification
     * @return the entity named by the bound event, or {@code null}
     */
    public static String eventEntity(NotificationIntent notification) {
        String kind = eventKind(notification);
        Object target = kind == null ? null
                : notification.getEvent()
                              .get(kind);
        return target == null ? null : target.toString();
    }

    /**
     * The suffix that distinguishes the per-operation topic the Java DAO publishes to: create uses the
     * unsuffixed base topic, update/delete use {@code -updated}/{@code -deleted}.
     *
     * @param eventKind the lifecycle event kind
     * @return the topic suffix, possibly empty
     */
    public static String topicSuffix(String eventKind) {
        if ("onUpdate".equals(eventKind)) {
            return "-updated";
        }
        if ("onDelete".equals(eventKind)) {
            return "-deleted";
        }
        return "";
    }

    /**
     * @param to the recipient ({@code member@x.com} literal or a direct field name)
     * @return a Java {@code String} expression for the recipient
     */
    public static String recipientExpression(String to) {
        if (to == null || to.isBlank()) {
            return "null";
        }
        String trimmed = to.trim();
        if (trimmed.contains("@")) {
            return quote(trimmed);
        }
        if (IDENTIFIER.matcher(trimmed)
                      .matches()) {
            return "entity." + IntentNaming.pascalCase(trimmed);
        }
        return quote(trimmed);
    }

    /**
     * Render text with {@code {field}} placeholders into a Java {@code String} expression - literal
     * segments are quoted, placeholders become {@code entity.<PascalField>} accesses.
     *
     * @param text the subject/body template
     * @return a Java {@code String} expression
     */
    public static String interpolate(String text) {
        if (text == null || text.isEmpty()) {
            return "\"\"";
        }
        List<String> terms = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                terms.add(quote(text.substring(last, matcher.start())));
            }
            terms.add("entity." + IntentNaming.pascalCase(matcher.group(1)));
            last = matcher.end();
        }
        if (last < text.length()) {
            terms.add(quote(text.substring(last)));
        }
        if (terms.isEmpty()) {
            return "\"\"";
        }
        // Force a String result when the only term is a field access (which might be non-String).
        if (terms.size() == 1 && terms.get(0)
                                      .startsWith("entity.")) {
            return "\"\" + " + terms.get(0);
        }
        return String.join(" + ", terms);
    }

    /**
     * Translate a {@code when} guard into a Java boolean expression. v1 supports a single comparison
     * {@code field == 'literal'} / {@code field != 'literal'}; anything else (or blank) yields
     * {@code true} (fire on every event).
     *
     * @param when the guard expression, may be {@code null}
     * @return a Java boolean expression
     */
    public static String guard(String when) {
        if (when == null || when.isBlank()) {
            return "true";
        }
        Matcher matcher = SIMPLE_COMPARISON.matcher(when);
        if (!matcher.matches()) {
            return "true";
        }
        String field = "entity." + IntentNaming.pascalCase(matcher.group(1));
        String rhs = literalToJava(matcher.group(3)
                                          .trim());
        String equals = "java.util.Objects.equals(" + field + ", " + rhs + ")";
        return "==".equals(matcher.group(2)) ? equals : "!" + equals;
    }

    private static String literalToJava(String rhs) {
        if (rhs.length() >= 2 && (rhs.startsWith("'") && rhs.endsWith("'") || rhs.startsWith("\"") && rhs.endsWith("\""))) {
            return quote(rhs.substring(1, rhs.length() - 1));
        }
        if (rhs.matches("-?\\d+(\\.\\d+)?") || "true".equals(rhs) || "false".equals(rhs)) {
            return rhs;
        }
        return quote(rhs);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\")
                           .replace("\"", "\\\"")
                           .replace("\n", "\\n")
                           .replace("\r", "")
                + "\"";
    }
}
