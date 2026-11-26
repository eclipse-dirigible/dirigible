/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.db;

import java.util.Map;

/**
 * Builds the refusals a generated controller answers a value of one property with - a required
 * value left out, a text longer than its column, a value off its pattern, a value that is not of
 * its named format, a value another record already holds under a {@code unique: true}, a required
 * single flag left without a holder, a refusing single flag another record holds - their messages
 * resolved for the language of the request exactly as a generated check's are, see
 * {@link CheckMessages} (dirigible #8596).
 *
 * <p>
 * Each sentence is an entry of the entity's model catalog, under {@code <catalog>.checks.<key>},
 * and names the property and the entity by their labels in that catalog
 * ({@code <catalog>.t.<name>}). A language catalog that does not carry an entry falls back to the
 * default-language text below, and a label without a catalog key to the English label it is given.
 * Every refusal names the property it is about, which the REST error body carries as {@code field},
 * so a form marks that field in any language.
 */
public final class FieldRefusals {

    /** The key, under a catalog's {@code checks}, of the sentence refusing a missing required value. */
    public static final String REQUIRED_KEY = "fieldRequired";

    /**
     * The default-language sentence refusing a missing required value: {@code {label}} is the field's.
     */
    public static final String REQUIRED_TEXT = "The '{label}' property is required";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a too long text. */
    public static final String TOO_LONG_KEY = "fieldTooLong";

    /** The default-language sentence refusing a too long text: {@code {length}} is the column's. */
    public static final String TOO_LONG_TEXT = "The '{label}' exceeds the maximum length of {length}";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a value off its pattern. */
    public static final String PATTERN_KEY = "fieldPattern";

    /**
     * The default-language sentence refusing a value off its pattern: {@code {pattern}} is the regex.
     */
    public static final String PATTERN_TEXT = "The value of '{label}' does not match the required pattern '{pattern}'";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a value that is no IBAN. */
    public static final String IBAN_KEY = "fieldIban";

    /** The default-language sentence refusing a value that is no IBAN (dirigible #8302). */
    public static final String IBAN_TEXT = "The value of '{label}' is not a valid IBAN";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a value that is no month. */
    public static final String MONTH_KEY = "fieldMonth";

    /** The default-language sentence refusing a value that is no month (dirigible #8328). */
    public static final String MONTH_TEXT = "The value of '{label}' is not a month of the form YYYY-MM";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a value that is no week. */
    public static final String WEEK_KEY = "fieldWeek";

    /** The default-language sentence refusing a value that is no ISO week (dirigible #8328). */
    public static final String WEEK_TEXT = "The value of '{label}' is not a week of the form YYYY-Www";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a repeated unique value. */
    public static final String DUPLICATE_KEY = "fieldDuplicate";

    /**
     * The default-language sentence refusing a repeated unique value: {@code {entity}} is the entity's.
     */
    public static final String DUPLICATE_TEXT = "A {entity} with this '{label}' already exists";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing to leave a {@code single: {
     * required: true }} flag without a holder.
     */
    public static final String SINGLE_REQUIRED_KEY = "fieldSingleRequired";

    /**
     * The default-language sentence refusing to leave a required single flag without a holder:
     * {@code {entity}} is the entity's label, {@code {label}} the flag's (dirigible #8700).
     */
    public static final String SINGLE_REQUIRED_TEXT = "One {entity} must stay '{label}' - mark another one '{label}' first";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a {@code single: {
     * onConflict: refuse }} flag another record holds.
     */
    public static final String SINGLE_TAKEN_KEY = "fieldSingleTaken";

    /**
     * The default-language sentence refusing a fleet-wide refusing single flag another record holds:
     * {@code {entity}} is the entity's label, {@code {label}} the flag's (dirigible #8101).
     */
    public static final String SINGLE_TAKEN_TEXT = "Another {entity} is already '{label}'";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a refusing single flag
     * another record of the same scope ({@code per:}) holds.
     */
    public static final String SINGLE_TAKEN_IN_SCOPE_KEY = "fieldSingleTakenInScope";

    /**
     * The default-language sentence refusing a scoped refusing single flag another record of the scope
     * holds: {@code {scope}} is the label of the relation the flag is scoped by (dirigible #8101).
     */
    public static final String SINGLE_TAKEN_IN_SCOPE_TEXT = "Another {entity} is already '{label}' for this {scope}";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a malformed selection. */
    public static final String SELECTION_KEY = "fieldSelection";

    /**
     * The default-language sentence refusing a multi-select value that is not the chosen ids,
     * comma-separated (dirigible #8775).
     */
    public static final String SELECTION_TEXT = "The '{label}' selection must be the chosen ids, comma-separated";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a reference to a non-leaf.
     */
    public static final String LEAF_ONLY_KEY = "fieldLeafOnly";

    /**
     * The default-language sentence refusing a {@code leafOnly} reference to a record with children:
     * {@code {target}} is the referenced entity's label (dirigible #8775).
     */
    public static final String LEAF_ONLY_TEXT = "The '{label}' must reference a leaf {target} - the selected one has children";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a hierarchy cycle. */
    public static final String HIERARCHY_CYCLE_KEY = "fieldHierarchyCycle";

    /**
     * The default-language sentence refusing a parent reference that would close a cycle:
     * {@code {entity}} is the hierarchy's entity label (dirigible #8775).
     */
    public static final String HIERARCHY_CYCLE_TEXT = "The '{label}' reference would create a cycle in the {entity} hierarchy";

    /** The key, under a catalog's {@code checks}, of the sentence refusing a direct status edit. */
    public static final String WORKFLOW_OWNED_KEY = "fieldWorkflowOwned";

    /**
     * The default-language sentence refusing a direct edit of the status a workflow owns (dirigible
     * #8775).
     */
    public static final String WORKFLOW_OWNED_TEXT = "'{label}' changes through the workflow, not a direct edit";

    private FieldRefusals() {}

    /**
     * The refusal of a required value left out.
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException required(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, REQUIRED_KEY, REQUIRED_TEXT, field, label, labelKey);
    }

    /**
     * The refusal of a text longer than its column.
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param length the column's length
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException tooLong(String catalog, String field, String label, String labelKey, int length) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, TOO_LONG_KEY, TOO_LONG_TEXT, field, label, labelKey,
                "length", length);
    }

    /**
     * The refusal of a value that does not match its pattern.
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param pattern the regular expression the value must match
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException pattern(String catalog, String field, String label, String labelKey, String pattern) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, PATTERN_KEY, PATTERN_TEXT, field, label, labelKey,
                "pattern", pattern);
    }

    /**
     * The refusal of a value of a {@code format: iban} field that is not an IBAN - off its shape, or
     * with check digits that do not hold (see {@link FieldFormats#isIban(String)}).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException iban(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, IBAN_KEY, IBAN_TEXT, field, label, labelKey);
    }

    /**
     * The refusal of a value of a {@code month} field that is not a month of the form {@code YYYY-MM}
     * (see {@link FieldFormats#isMonth(String)}).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException month(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, MONTH_KEY, MONTH_TEXT, field, label, labelKey);
    }

    /**
     * The refusal of a value of a {@code week} field that is not an ISO week of the form
     * {@code YYYY-Www} (see {@link FieldFormats#isWeek(String)}).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException week(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, WEEK_KEY, WEEK_TEXT, field, label, labelKey);
    }

    /**
     * The refusal of a value that does not match its pattern, in the words the model's author gave it
     * (intent {@code patternMessage:}, dirigible #7749) instead of the generic sentence that quotes the
     * regex. The message may name the property as {@code {label}} and the regex as {@code {pattern}}.
     *
     * @param messageKey the fully qualified catalog key of the author's message, or {@code null} for
     *        the default-language text
     * @param message the author's message in the default language
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param pattern the regular expression the value must match
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException authoredPattern(String messageKey, String message, String field, String label, String labelKey,
            String pattern) {
        return authoredPattern(CheckMessages.language(), CheckMessages::readCatalog, messageKey, message, field, label, labelKey, pattern);
    }

    static ValidationException authoredPattern(String language, CheckMessages.CatalogReader catalogs, String messageKey, String message,
            String field, String label, String labelKey, String pattern) {
        Map<String, Object> values =
                CheckMessages.params("label", CheckMessages.resolve(language, catalogs, labelKey, label, Map.of()), "pattern", pattern);
        return new ValidationException(CheckMessages.resolve(language, catalogs, messageKey, message, values), messageKey, values, field);
    }

    /**
     * The refusal of a value another record already holds under a {@code unique: true} - the reason a
     * generated controller answers its {@code 409 Conflict} with.
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param entityLabel the entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the refusal, carrying the key, the parameters and the field
     */
    public static ValidationException duplicate(String catalog, String entityLabel, String entityLabelKey, String field, String label,
            String labelKey) {
        return duplicate(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey, field, label,
                labelKey);
    }

    static ValidationException duplicate(String language, CheckMessages.CatalogReader catalogs, String catalog, String entityLabel,
            String entityLabelKey, String field, String label, String labelKey) {
        return refusal(language, catalogs, catalog, DUPLICATE_KEY, DUPLICATE_TEXT, field, label, labelKey, "entity",
                CheckMessages.resolve(language, catalogs, entityLabelKey, entityLabel, Map.of()));
    }

    /**
     * The refusal of a write that would leave a {@code single: { required: true }} flag held by no row
     * of its scope - its holder un-flagged, moved out of the scope or deleted while other rows remain
     * (dirigible #8700).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param entityLabel the entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @param field the flag's property
     * @param label the flag's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException singleRequired(String catalog, String entityLabel, String entityLabelKey, String field, String label,
            String labelKey) {
        return singleRequired(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey, field, label,
                labelKey);
    }

    static ValidationException singleRequired(String language, CheckMessages.CatalogReader catalogs, String catalog, String entityLabel,
            String entityLabelKey, String field, String label, String labelKey) {
        return refusal(language, catalogs, catalog, SINGLE_REQUIRED_KEY, SINGLE_REQUIRED_TEXT, field, label, labelKey, "entity",
                CheckMessages.resolve(language, catalogs, entityLabelKey, entityLabel, Map.of()));
    }

    /**
     * The refusal of a write that sets a fleet-wide {@code single: { onConflict: refuse }} flag while
     * another record holds it (dirigible #8101).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param entityLabel the entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @param field the flag's property
     * @param label the flag's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the refusal - carrying the key, the parameters and the field; wrapped in a
     *         {@link DuplicateKeyException} it is answered HTTP 409
     */
    public static ValidationException singleTaken(String catalog, String entityLabel, String entityLabelKey, String field, String label,
            String labelKey) {
        return singleTaken(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey, field, label,
                labelKey);
    }

    static ValidationException singleTaken(String language, CheckMessages.CatalogReader catalogs, String catalog, String entityLabel,
            String entityLabelKey, String field, String label, String labelKey) {
        return refusal(language, catalogs, catalog, SINGLE_TAKEN_KEY, SINGLE_TAKEN_TEXT, field, label, labelKey, "entity",
                CheckMessages.resolve(language, catalogs, entityLabelKey, entityLabel, Map.of()));
    }

    /**
     * The refusal of a write that sets a {@code single: { per: <relation>, onConflict: refuse }} flag
     * while another record of the same scope holds it (dirigible #8101).
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param entityLabel the entity's default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @param field the flag's property
     * @param label the flag's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param scopeLabel the default-language label of the relation the flag is scoped by
     * @param scopeLabelKey the fully qualified catalog key of that label, or {@code null}
     * @return the refusal - carrying the key, the parameters and the field; wrapped in a
     *         {@link DuplicateKeyException} it is answered HTTP 409
     */
    public static ValidationException singleTakenInScope(String catalog, String entityLabel, String entityLabelKey, String field,
            String label, String labelKey, String scopeLabel, String scopeLabelKey) {
        return singleTakenInScope(CheckMessages.language(), CheckMessages::readCatalog, catalog, entityLabel, entityLabelKey, field, label,
                labelKey, scopeLabel, scopeLabelKey);
    }

    static ValidationException singleTakenInScope(String language, CheckMessages.CatalogReader catalogs, String catalog, String entityLabel,
            String entityLabelKey, String field, String label, String labelKey, String scopeLabel, String scopeLabelKey) {
        return refusal(language, catalogs, catalog, SINGLE_TAKEN_IN_SCOPE_KEY, SINGLE_TAKEN_IN_SCOPE_TEXT, field, label, labelKey, "entity",
                CheckMessages.resolve(language, catalogs, entityLabelKey, entityLabel, Map.of()), "scope",
                CheckMessages.resolve(language, catalogs, scopeLabelKey, scopeLabel, Map.of()));
    }

    /**
     * The refusal of a multi-select value that is not the chosen ids, comma-separated.
     *
     * @param catalog the entity's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param field the property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException selection(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, SELECTION_KEY, SELECTION_TEXT, field, label,
                labelKey);
    }

    /**
     * The refusal of a {@code leafOnly} reference to a record that has children.
     *
     * @param catalog the entity's model catalog, or {@code null}
     * @param field the reference property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param targetLabel the referenced entity's default-language label
     * @param targetLabelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException leafOnly(String catalog, String field, String label, String labelKey, String targetLabel,
            String targetLabelKey) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, LEAF_ONLY_KEY, LEAF_ONLY_TEXT, field, label, labelKey, "target",
                CheckMessages.resolve(language, CheckMessages::readCatalog, targetLabelKey, targetLabel, Map.of()));
    }

    /**
     * The refusal of a parent reference that would make a record its own ancestor.
     *
     * @param catalog the entity's model catalog, or {@code null}
     * @param field the parent reference property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @param entityLabel the hierarchy's entity default-language label
     * @param entityLabelKey the fully qualified catalog key of that label, or {@code null}
     * @return the exception to throw - mapped to HTTP 400 carrying the key, the parameters and the
     *         field
     */
    public static ValidationException hierarchyCycle(String catalog, String field, String label, String labelKey, String entityLabel,
            String entityLabelKey) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, HIERARCHY_CYCLE_KEY, HIERARCHY_CYCLE_TEXT, field, label, labelKey,
                "entity", CheckMessages.resolve(language, CheckMessages::readCatalog, entityLabelKey, entityLabel, Map.of()));
    }

    /**
     * The refusal of a direct edit of the status a workflow owns - a {@code processes:} step or a
     * {@code transitions:} button.
     *
     * @param catalog the entity's model catalog, or {@code null}
     * @param field the status property
     * @param label the property's default-language label
     * @param labelKey the fully qualified catalog key of that label, or {@code null}
     * @return the refusal, carrying the key, the parameters and the field; wrapped in a 409 by the
     *         generated controllers
     */
    public static ValidationException workflowOwned(String catalog, String field, String label, String labelKey) {
        return refusal(CheckMessages.language(), CheckMessages::readCatalog, catalog, WORKFLOW_OWNED_KEY, WORKFLOW_OWNED_TEXT, field, label,
                labelKey);
    }

    static ValidationException refusal(String language, CheckMessages.CatalogReader catalogs, String catalog, String key, String text,
            String field, String label, String labelKey, Object... params) {
        Object[] all = new Object[params.length + 2];
        all[0] = "label";
        all[1] = CheckMessages.resolve(language, catalogs, labelKey, label, Map.of());
        System.arraycopy(params, 0, all, 2, params.length);
        Map<String, Object> values = CheckMessages.params(all);
        String messageKey = catalog == null || catalog.isBlank() ? null : catalog + ".checks." + key;
        return new ValidationException(CheckMessages.resolve(language, catalogs, messageKey, text, values), messageKey, values, field);
    }
}
