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

import java.util.Locale;
import java.util.Map;

/**
 * Naming conventions shared by every intent generator. The physical table name in particular is
 * referenced from three artefacts (the {@code .edm} entity {@code dataName}, the {@code .report}
 * {@code baseTable} and the {@code .csvim} {@code table}) - all three must call
 * {@link #tableName(IntentGenerationContext, String)} so they can never drift apart.
 */
public final class IntentNaming {

    private IntentNaming() {}

    /**
     * Identifiers whose human-readable singular form cannot be produced by case-boundary splitting -
     * acronyms and mixed-case domain terms (keyed lower-case). {@code UoM} -> {@code Unit of Measure}.
     */
    private static final Map<String, String> HUMANIZE_OVERRIDES = Map.of("uom", "Unit of Measure");

    /**
     * Plural overrides for (humanized) labels whose last word must not be naively pluralized (keyed
     * lower-case). Both the humanized singular and the raw identifier map to the same plural.
     */
    private static final Map<String, String> PLURALIZE_OVERRIDES = Map.of("unit of measure", "Units of Measure", "uom", "Units of Measure");

    /**
     * The intent's base name used for single-file outputs ({@code <base>.edm}, {@code <base>.roles})
     * and as the physical table-name prefix. The YAML document's own {@code name:} field wins - the
     * file is conventionally called {@code app.intent}, so the name derived from the file name
     * ({@code app}) is a poor identity. Falls back to the intent file's base name, then the project
     * name, then the literal {@code intent}.
     *
     * @param context the generation context
     * @return the base name, never blank
     */
    public static String baseName(IntentGenerationContext context) {
        String declaredName = context.getModel()
                                     .getName();
        if (declaredName != null && !declaredName.isBlank()) {
            return declaredName;
        }
        String fallbackName = context.getFallbackName();
        if (fallbackName != null && !fallbackName.isBlank()) {
            return fallbackName;
        }
        String project = context.getProjectName();
        return project.isEmpty() ? "intent" : project;
    }

    /**
     * Physical table name for an entity: {@code <INTENT>_<ENTITY>} in upper snake (e.g.
     * {@code ORDERS_COUNTRY}). The intent-name prefix keeps tables unique across projects sharing a
     * schema and away from SQL reserved words like {@code ORDER}.
     *
     * @param context the generation context
     * @param entityName the entity's declared name
     * @return the upper-snake, intent-prefixed table name
     */
    public static String tableName(IntentGenerationContext context, String entityName) {
        return upperSnake(baseName(context)) + "_" + upperSnake(entityName);
    }

    /**
     * Capitalize the first letter to make an UpperCamelCase (PascalCase) name, preserving the rest -
     * the Dirigible EDM convention for property names ({@code id} -> {@code Id}, {@code loanedOn} ->
     * {@code LoanedOn}). Authoring stays lower camelCase; only the generated model property names are
     * PascalCased (column {@code dataName}s stay UPPER_SNAKE).
     *
     * @param name the identifier to convert (may be null)
     * @return the PascalCase form, empty for null/empty input
     */
    public static String pascalCase(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * A valid PascalCase Java identifier from an arbitrary authored name: splits on every run of
     * non-alphanumeric separators ({@code -}, {@code _}, space, {@code .}) and capitalizes each
     * segment, so a kebab-case action name becomes a legal class name ({@code order-from-quote} ->
     * {@code OrderFromQuote}). A pure camelCase input just gets its first letter capitalized
     * ({@code orderFromQuote} -> {@code OrderFromQuote}); unlike {@link #pascalCase(String)} this never
     * leaves a separator in the result.
     *
     * @param name the identifier to convert (may be null)
     * @return the PascalCase identifier, empty for null/empty input
     */
    public static String pascalIdentifier(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                capitalizeNext = true;
                continue;
            }
            if (capitalizeNext) {
                out.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Camel-/Pascal-case to upper snake. Handles {@code IDValue} -> {@code ID_VALUE}, and collapses any
     * run of non-alphanumeric separators ({@code -}, space, {@code .}, {@code /}) to a single
     * underscore so a kebab-case intent/project name produces a <b>valid SQL identifier</b>:
     * {@code sales-invoices} -> {@code SALES_INVOICES}, not the invalid {@code SALES-INVOICES} (an
     * unquoted {@code -} is parsed as minus and breaks table creation). Leading/trailing separators do
     * not leave a dangling underscore. Pure-identifier input (entity / field names) is unaffected.
     *
     * @param name the identifier to convert (may be null)
     * @return the upper-snake form, empty for null/empty input
     */
    public static String upperSnake(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length() + 8);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                // Separator (-, space, ., /, ...): emit a single underscore, never doubled or leading.
                if (out.length() > 0 && out.charAt(out.length() - 1) != '_') {
                    out.append('_');
                }
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1)) && out.length() > 0
                    && out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
            out.append(Character.toUpperCase(c));
        }
        // A trailing separator would leave a dangling underscore.
        if (out.length() > 0 && out.charAt(out.length() - 1) == '_') {
            out.setLength(out.length() - 1);
        }
        return out.toString();
    }

    /**
     * Lower-case the first letter to make a lowerCamelCase name, preserving the rest - the mirror of
     * {@link #pascalCase(String)} ({@code ResolveBookPrice} -> {@code resolveBookPrice}). Used to
     * normalise generated identifiers (e.g. a PascalCase handler) to the lower-camel form authored step
     * names already use, so BPMN element ids are uniform.
     *
     * @param name the identifier to convert (may be null)
     * @return the lowerCamelCase form, empty for null/empty input
     */
    public static String camelCase(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Turn a camel-/Pascal-case identifier into a human-readable Title Case label by splitting on case
     * boundaries ({@code librarianReview} -> {@code Librarian Review}, {@code LoanApproval} ->
     * {@code Loan Approval}). Used for BPMN display names (process and task {@code name}) while the
     * machine ids stay the compact identifier.
     *
     * @param name the identifier to humanize (may be null)
     * @return the spaced Title Case label, empty for null/empty input
     */
    public static String humanize(String name) {
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
     * Pluralizes the last word of a (already humanized) label using simple English rules - used for
     * navigation / menu labels so the sidebar reads naturally (e.g. {@code "Sales Invoice"} ->
     * {@code "Sales Invoices"}, {@code "Category"} -> {@code "Categories"}, {@code "Book"} ->
     * {@code "Books"}).
     *
     * @param label the label whose last word to pluralize (may be null)
     * @return the label with its last word pluralized, empty for null/empty input
     */
    public static String pluralize(String label) {
        if (label == null || label.isEmpty()) {
            return "";
        }
        String override = PLURALIZE_OVERRIDES.get(label.toLowerCase(Locale.ROOT));
        if (override != null) {
            return override;
        }
        int sp = label.lastIndexOf(' ');
        String head = sp >= 0 ? label.substring(0, sp + 1) : "";
        String last = sp >= 0 ? label.substring(sp + 1) : label;
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
}
