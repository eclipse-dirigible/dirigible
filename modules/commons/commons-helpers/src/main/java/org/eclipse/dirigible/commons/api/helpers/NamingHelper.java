/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.api.helpers;

import java.util.Locale;
import java.util.Map;

/**
 * Naming rules shared by every code-generation path: turning identifiers into display labels,
 * pluralizing a label, and sanitizing a name into a Java identifier.
 *
 * <p>
 * These rules are consumed both by the intent generators and by the model-generation pipeline,
 * which must agree on the labels they bake into generated artefacts and translation catalogs. This
 * class is the single implementation; it lives here because it is pure text handling with no
 * dependencies.
 *
 * <p>
 * Note the deliberate distinction between {@link #humanizeIdentifier(String)} and
 * {@link #humanizeName(String)}: only the latter treats {@code -} and {@code _} as word separators.
 * The model-generation pipeline uses the former, because that is what its (previously JavaScript)
 * implementation did, and changing it would silently relabel every generated artefact whose
 * property names contain separators.
 */
public final class NamingHelper {

    /**
     * Labels for identifiers whose humanized form is not derivable from their casing, keyed lower-case.
     */
    private static final Map<String, String> HUMANIZE_OVERRIDES = Map.of("uom", "Unit of Measure");

    /**
     * Plurals for (humanized) labels whose last word must not be naively pluralized, keyed lower-case.
     * Both the humanized singular and the raw identifier map to the same plural.
     */
    private static final Map<String, String> PLURALIZE_OVERRIDES = Map.of("unit of measure", "Units of Measure", "uom", "Units of Measure");

    /**
     * Not instantiable.
     */
    private NamingHelper() {}

    /**
     * Humanizes a single PascalCase or camelCase identifier for display, inserting a space before each
     * upper-case letter that starts a word ({@code "TaxEventDate"} becomes {@code "Tax Event Date"}).
     * Separators are NOT treated as word boundaries - see {@link #humanizeName(String)}.
     *
     * @param name the identifier, may be null
     * @return the display label, never null
     */
    public static String humanizeIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String override = HUMANIZE_OVERRIDES.get(name.toLowerCase(Locale.ROOT));
        if (override != null) {
            return override;
        }
        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                out.append(' ');
            }
            out.append(i == 0 ? Character.toUpperCase(c) : c);
        }
        return out.toString();
    }

    /**
     * Humanizes a name for display, additionally treating {@code -} and {@code _} as word separators
     * ({@code "payment_method"} becomes {@code "Payment Method"}). Each segment is humanized with
     * {@link #humanizeIdentifier(String)}.
     *
     * @param name the name, may be null
     * @return the display label, never null
     */
    public static String humanizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String override = HUMANIZE_OVERRIDES.get(name.toLowerCase(Locale.ROOT));
        if (override != null) {
            return override;
        }
        if (name.indexOf('-') < 0 && name.indexOf('_') < 0) {
            return humanizeIdentifier(name);
        }
        StringBuilder joined = new StringBuilder(name.length() + 8);
        for (String segment : name.split("[-_]+")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(humanizeName(segment));
        }
        return joined.toString();
    }

    /**
     * Pluralizes a humanized label's last word ({@code "Sales Invoice"} becomes
     * {@code "Sales Invoices"}, {@code "Country"} becomes {@code "Countries"}).
     *
     * @param label the singular label, may be null
     * @return the plural label, never null
     */
    public static String pluralizeLabel(String label) {
        if (label == null || label.isEmpty()) {
            return "";
        }
        String override = PLURALIZE_OVERRIDES.get(label.toLowerCase(Locale.ROOT));
        if (override != null) {
            return override;
        }
        int space = label.lastIndexOf(' ');
        String head = space >= 0 ? label.substring(0, space + 1) : "";
        String last = space >= 0 ? label.substring(space + 1) : label;
        if (last.isEmpty()) {
            return label;
        }
        String lower = last.toLowerCase(Locale.ROOT);
        String plural;
        if (lower.length() > 1 && lower.endsWith("y") && "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
            plural = last.substring(0, last.length() - 1) + "ies";
        } else if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch") || lower.endsWith("sh")) {
            plural = last + "es";
        } else {
            plural = last + "s";
        }
        return head + plural;
    }

    /**
     * Sanitizes an arbitrary name (a perspective, a model folder) into a lower-case Java identifier
     * safe for use both as a path segment and as a package fragment.
     *
     * @param name the name, may be null
     * @return the sanitized identifier, never blank
     */
    public static String sanitizeJavaIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "_";
        }
        StringBuilder out = new StringBuilder(name.length() + 1);
        String lower = name.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        if (out.length() == 0) {
            return "_";
        }
        char first = out.charAt(0);
        if (first >= '0' && first <= '9') {
            out.insert(0, '_');
        }
        return out.toString();
    }

}
