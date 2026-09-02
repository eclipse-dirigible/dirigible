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

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;

import java.util.Locale;

/**
 * Naming conventions shared by every intent generator. The physical table name in particular is
 * referenced from three artefacts (the {@code .edm} entity {@code dataName}, the {@code .report}
 * {@code baseTable} and the {@code .csvim} {@code table}) - all three must call
 * {@link #tableName(IntentGenerationContext, String)} so they can never drift apart.
 */
public final class IntentNaming {

    private IntentNaming() {}

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
    /**
     * A custom action's (transition / generate) display label: the authored {@code label:}, else the
     * humanized action name. One rule for the descriptor AND the translation catalog, so the catalog
     * entry always matches what the button falls back to.
     *
     * @param name the action name
     * @param label the authored label, may be {@code null}/blank
     * @return the display label
     */
    public static String customActionLabel(String name, String label) {
        return label == null || label.isBlank() ? humanize(name) : label;
    }

    /**
     * The i18n catalog key of a custom action's label: {@code <project>:<model>-model.actions.<name>} -
     * the same {@code <model>-model} namespace the generated translation catalog is emitted under
     * (mirrors the template engine's translation prefix for the {@code .model} file).
     *
     * @param project the project name
     * @param context the generation context (for the model base name)
     * @param name the action name
     * @return the translation key
     */
    public static String customActionTranslationKey(String project, IntentGenerationContext context, String name) {
        return project + ":" + baseName(context) + "-model.actions." + name;
    }

    /**
     * The i18n catalog the module's BPM user-task labels live in:
     * {@code <project>:<model>-model.processes} - the {@code processes} section the translation catalog
     * is emitted with, the sibling of {@link #customActionTranslationKey}. A task's own key is this
     * catalog plus the task's BPMN id, which is the authored step name; the generated {@code .bpmn}
     * declares the catalog on its {@code <process>}, so the Inbox can name a task in the user's
     * language without knowing which module raised it.
     *
     * @param project the project name
     * @param context the generation context (for the model base name)
     * @return the catalog prefix, without a trailing dot
     */
    public static String processTaskCatalog(String project, IntentGenerationContext context) {
        return project + ":" + baseName(context) + "-model.processes";
    }

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
     * The intent's sanitized Java module segment - the exact mirror of the template engine's
     * {@code NamingHelper.sanitizeJavaIdentifier}: lower-cased, every character outside
     * {@code [a-z0-9_]} replaced by an underscore, a leading digit prefixed with an underscore
     * ({@code sales-invoices} -> {@code sales_invoices}). Producer (this engine, which writes handler
     * FQNs into the {@code .bpmn} and endpoint URLs into extensions) and consumer (the events template,
     * which emits the {@code package} line) must derive the same segment or the generated references
     * never match.
     *
     * @param context the generation context
     * @return the sanitized module segment, never blank
     */
    public static String javaModule(IntentGenerationContext context) {
        String name = baseName(context).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(name.length() + 1);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' ? c : '_');
        }
        if (out.length() == 0) {
            return "_";
        }
        if (Character.isDigit(out.charAt(0))) {
            out.insert(0, '_');
        }
        return out.toString();
    }

    /**
     * The module-scoped Java package every generated event handler lands in:
     * {@code gen.events.<module>} (e.g. {@code gen.events.sales_invoices}). Namespacing per module
     * keeps two modules that author a same-named reaction (both built from the same recipe) from
     * colliding by FQN in the registry-wide client-Java compilation. The module segment goes UNDER
     * {@code gen/events} - not {@code gen/<module>/events} - so the glue output stays a sibling of the
     * model-to-code template's {@code gen/<module>} folder and survives a model-only regeneration
     * (which wipes {@code gen/<module>} wholesale).
     *
     * @param context the generation context
     * @return the events package for this intent module
     */
    public static String eventsPackage(IntentGenerationContext context) {
        return "gen.events." + javaModule(context);
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
     * {@code Loan Approval}). Hyphens and underscores are word separators too, so kebab-case project
     * names read naturally ({@code sales-invoices} -> {@code Sales Invoices}, not
     * {@code Sales-invoices}). Used for BPMN display names (process and task {@code name}) and as the
     * default app/brand title, while the machine ids stay the compact identifier.
     *
     * @param name the identifier to humanize (may be null)
     * @return the spaced Title Case label, empty for null/empty input
     */
    public static String humanize(String name) {
        return NamingHelper.humanizeName(name);
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
        return NamingHelper.pluralizeLabel(label);
    }

    /**
     * The parent property a capacity roll-up keeps the DISPLACED status in: the status the parent held
     * before the roll-up first moved it into {@code statusWhenFull} / {@code statusWhenPartial}, put
     * back when the summed children go away again (#7016). Named after the status relation so two
     * roll-ups driving different status relations of one parent keep separate memories.
     *
     * @param statusRelation the roll-up's {@code status:} relation name
     * @return the PascalCase property name, e.g. {@code DisplacedStatus}
     */
    public static String displacedStatusProperty(String statusRelation) {
        return "Displaced" + pascalCase(statusRelation);
    }
}
