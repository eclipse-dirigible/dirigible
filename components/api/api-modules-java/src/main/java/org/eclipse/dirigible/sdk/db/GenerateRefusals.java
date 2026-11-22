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

import java.util.List;
import java.util.Map;

/**
 * Builds the refusals a generated create-from (intent {@code generates:}) answers a run with - a
 * source the report it reads cannot be run for, no row qualifying, a row its rule refuses, a report
 * value the target cannot hold - their messages resolved for the language of the request exactly as
 * a generated check's are, see {@link CheckMessages} (dirigible #8646).
 *
 * <p>
 * Each sentence is an entry of the intent's model catalog, under {@code <catalog>.checks.<key>},
 * and names the entities and the action by their labels in that catalog
 * ({@code <catalog>.t.<name>}, {@code <catalog>.actions.<name>}). A language catalog that does not
 * carry an entry falls back to the default-language text below, and a label without a catalog key
 * to the English label it is given.
 */
public final class GenerateRefusals {

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a source that lacks a value
     * the report the create-from reads is run with.
     */
    public static final String REPORT_VALUE_MISSING_KEY = "generateReportValueMissing";

    /**
     * The default-language sentence refusing a source that lacks a report parameter's value:
     * {@code {entity}} is the source's label, {@code {id}} its key, {@code {property}} the property the
     * parameter reads, {@code {report}} the report and {@code {parameter}} the parameter.
     */
    public static final String REPORT_VALUE_MISSING_TEXT =
            "{entity} [{id}] has no {property}, which report {report} is run with as {parameter}";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a run no report row
     * qualifies.
     */
    public static final String NOTHING_QUALIFIES_KEY = "generateNothingQualifies";

    /**
     * The default-language sentence refusing a run no report row qualifies: {@code {action}} is the
     * create-from's label.
     */
    public static final String NOTHING_QUALIFIES_TEXT = "no row of report {report} qualifies for {action}, so there is nothing to generate";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a run whose source has no
     * item row its rule qualifies.
     */
    public static final String NO_ITEM_QUALIFIES_KEY = "generateNoItemQualifies";

    /**
     * The default-language sentence refusing a run whose source has no qualifying item row:
     * {@code {item}} is the item's label, {@code {entity}} the source's and {@code {target}} the
     * target's.
     */
    public static final String NO_ITEM_QUALIFIES_TEXT =
            "no {item} row of the {entity} qualifies for {action}, so the {target} would have no lines";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a run no report row gives a
     * line.
     */
    public static final String NO_REPORT_ROW_QUALIFIES_KEY = "generateNoReportRowQualifies";

    /** The default-language sentence refusing a run no report row gives a line. */
    public static final String NO_REPORT_ROW_QUALIFIES_TEXT =
            "no row of report {report} qualifies for {action}, so the {target} would have no lines";

    /**
     * The key, under a catalog's {@code checks}, of the note naming the source row a refused target
     * line was mapped from.
     */
    public static final String FROM_ITEM_KEY = "generateFromItem";

    /**
     * The default-language note naming the source row of a refused target line: {@code {message}} is
     * the target's own refusal, {@code {item}} the source item's label and {@code {id}} its key.
     */
    public static final String FROM_ITEM_TEXT = "{message} (from {item} [{id}])";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a report value too large to
     * be read exactly.
     */
    public static final String REPORT_VALUE_TOO_LARGE_KEY = "generateReportValueTooLarge";

    /** The default-language sentence refusing a report value too large to be read exactly. */
    public static final String REPORT_VALUE_TOO_LARGE_TEXT = "report column [{column}] holds [{value}], too large to be read exactly";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a report value the target
     * field cannot hold without rounding.
     */
    public static final String REPORT_VALUE_ROUNDING_KEY = "generateReportValueRounding";

    /**
     * The default-language sentence refusing a report value the target field cannot hold without
     * rounding: {@code {scale}} is the field's decimal places.
     */
    public static final String REPORT_VALUE_ROUNDING_TEXT =
            "report column [{column}] holds [{value}], which a field of {scale} decimal places cannot hold without rounding";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a report value a whole-number
     * field cannot hold.
     */
    public static final String REPORT_VALUE_NOT_WHOLE_KEY = "generateReportValueNotWhole";

    /** The default-language sentence refusing a report value that is not a whole number. */
    public static final String REPORT_VALUE_NOT_WHOLE_TEXT = "report column [{column}] holds [{value}], which is not a whole number";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a second run while the rows
     * the first one generated stand.
     */
    public static final String ALREADY_GENERATED_ROWS_KEY = "generateAlreadyGeneratedRows";

    /**
     * The default-language sentence refusing a second run while its rows stand: {@code {count}} is how
     * many there are (dirigible #8775).
     */
    public static final String ALREADY_GENERATED_ROWS_TEXT =
            "{action} already generated {count} {target} rows from this {entity} - undo them before running it again";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing a second run while the
     * document the first one generated stands.
     */
    public static final String ALREADY_GENERATED_DOCUMENT_KEY = "generateAlreadyGeneratedDocument";

    /** The default-language sentence refusing a second run while its document stands. */
    public static final String ALREADY_GENERATED_DOCUMENT_TEXT =
            "{action} already generated {target} [{id}] from this {entity} - undo it before running it again";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing an undo with nothing to undo.
     */
    public static final String NOTHING_TO_UNDO_KEY = "generateNothingToUndo";

    /** The default-language sentence refusing an undo of a run that generated nothing. */
    public static final String NOTHING_TO_UNDO_TEXT = "{action} has generated no {target} from this {entity} - there is nothing to undo";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing an undo whose documents are
     * all cancelled or reversed already.
     */
    public static final String NOTHING_LIVE_TO_UNDO_KEY = "generateNothingLiveToUndo";

    /** The default-language sentence refusing an undo whose documents are all retired already. */
    public static final String NOTHING_LIVE_TO_UNDO_TEXT =
            "{action} has generated no live {target} from this {entity} - there is nothing to undo";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing an undo of a document a
     * reversal already reverses.
     */
    public static final String ALREADY_REVERSED_KEY = "generateAlreadyReversed";

    /**
     * The default-language sentence refusing an undo of a document already reversed: {@code {reversal}}
     * is the reversal's key.
     */
    public static final String ALREADY_REVERSED_TEXT = "{target} [{id}] is already reversed by [{reversal}] - post that reversal";

    /**
     * The key, under a catalog's {@code checks}, of the sentence refusing an undo of a document in a
     * status its reversal does not accept.
     */
    public static final String REVERSAL_STATUS_KEY = "generateReversalStatus";

    /**
     * The default-language sentence refusing an undo of a document in a status its reversal does not
     * accept: {@code {allowed}} are the statuses it accepts, {@code {status}} the document's.
     */
    public static final String REVERSAL_STATUS_TEXT =
            "{target} [{id}] is not in a status its reversal accepts ({allowed}) - it is {status}";

    /**
     * An entity or an action as a refusal names it.
     *
     * @param text the default-language label, e.g. {@code Stay Night}
     * @param key the fully qualified catalog key of that label, or {@code null} when it has none
     */
    public record Label(String text, String key) {
    }

    private GenerateRefusals() {}

    /**
     * The refusal of a source that lacks the value a report parameter is run with.
     *
     * @param catalog the intent's model catalog ({@code <project>:<catalog prefix>}), or {@code null}
     * @param entity the source entity
     * @param id the source's key
     * @param property the source property the parameter reads
     * @param report the report
     * @param parameter the report parameter
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException reportValueMissing(String catalog, Label entity, Object id, String property, String report,
            String parameter) {
        return reportValueMissing(CheckMessages.language(), CheckMessages::readCatalog, catalog, entity, id, property, report, parameter);
    }

    /**
     * The refusal of a run no report row qualifies for.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param report the report
     * @param action the create-from
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException nothingQualifies(String catalog, String report, Label action) {
        return nothingQualifies(CheckMessages.language(), CheckMessages::readCatalog, catalog, report, action);
    }

    /**
     * The refusal of a run whose source has no item row its rule qualifies.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param item the source's item entity
     * @param entity the source entity
     * @param action the create-from
     * @param target the target entity
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException noItemQualifies(String catalog, Label item, Label entity, Label action, Label target) {
        return noItemQualifies(CheckMessages.language(), CheckMessages::readCatalog, catalog, item, entity, action, target);
    }

    /**
     * The refusal of a run no report row gives a line.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param report the report
     * @param action the create-from
     * @param target the target entity
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException noReportRowQualifies(String catalog, String report, Label action, Label target) {
        return noReportRowQualifies(CheckMessages.language(), CheckMessages::readCatalog, catalog, report, action, target);
    }

    /**
     * The refusal of item rows the create-from's rule refuses (intent {@code items: refuse:}): the
     * author's sentence, translated under its own key, followed by the refused rows.
     *
     * @param messageKey the fully qualified catalog key of the author's sentence, or {@code null}
     * @param text the author's default-language sentence
     * @param item the source's item entity
     * @param rows the keys of the refused rows
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException itemsRefused(String messageKey, String text, Label item, List<Object> rows) {
        return itemsRefused(CheckMessages.language(), CheckMessages::readCatalog, messageKey, text, item, rows);
    }

    /**
     * The refusal of a target line, naming the source row it was mapped from. The target's own refusal
     * stays the cause, and its key, parameters and field are the ones the error body carries. A line
     * that collided with a business key stays a {@link DuplicateKeyException}.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param refused the target line's refusal
     * @param item the source's item entity
     * @param id the source row's key
     * @return the exception to throw - mapped to HTTP 400, or to 409 for a business-key collision
     */
    public static ValidationException fromItem(String catalog, ValidationException refused, Label item, Object id) {
        return fromItem(CheckMessages.language(), CheckMessages::readCatalog, catalog, refused, item, id);
    }

    /**
     * The refusal of a report value too large to be read exactly.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param column the report column
     * @param value the value it holds
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException reportValueTooLarge(String catalog, String column, Object value) {
        return reportValueTooLarge(CheckMessages.language(), CheckMessages::readCatalog, catalog, column, value);
    }

    /**
     * The refusal of a report value a whole-number field cannot hold.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param column the report column
     * @param value the value it holds
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException reportValueNotWhole(String catalog, String column, Object value) {
        return reportValueNotWhole(CheckMessages.language(), CheckMessages::readCatalog, catalog, column, value);
    }

    /**
     * The refusal of a report value the target field cannot hold without rounding.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param column the report column
     * @param value the value it holds
     * @param scale the target field's decimal places
     * @return the exception to throw - mapped to HTTP 400 carrying the key and the parameters
     */
    public static ValidationException reportValueRounding(String catalog, String column, Object value, int scale) {
        return reportValueRounding(CheckMessages.language(), CheckMessages::readCatalog, catalog, column, value, scale);
    }

    /**
     * The refusal of a second run while the rows the first one generated stand.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param action the create-from
     * @param count how many rows stand
     * @param target the generated entity
     * @param entity the source entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException alreadyGeneratedRows(String catalog, Label action, int count, Label target, Label entity) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, ALREADY_GENERATED_ROWS_KEY, ALREADY_GENERATED_ROWS_TEXT,
                CheckMessages.params("action", label(language, CheckMessages::readCatalog, action), "count", count, "target",
                        label(language, CheckMessages::readCatalog, target), "entity",
                        label(language, CheckMessages::readCatalog, entity)));
    }

    /**
     * The refusal of a second run while the document the first one generated stands.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param action the create-from
     * @param target the generated entity
     * @param id the standing document's key
     * @param entity the source entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException alreadyGeneratedDocument(String catalog, Label action, Label target, Object id, Label entity) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, ALREADY_GENERATED_DOCUMENT_KEY, ALREADY_GENERATED_DOCUMENT_TEXT,
                CheckMessages.params("action", label(language, CheckMessages::readCatalog, action), "target",
                        label(language, CheckMessages::readCatalog, target), "id", id, "entity",
                        label(language, CheckMessages::readCatalog, entity)));
    }

    /**
     * The refusal of an undo of a run that generated nothing - or nothing that is still live.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param live whether the run generated documents that are all retired already
     * @param action the create-from
     * @param target the generated entity
     * @param entity the source entity
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException nothingToUndo(String catalog, boolean live, Label action, Label target, Label entity) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, live ? NOTHING_LIVE_TO_UNDO_KEY : NOTHING_TO_UNDO_KEY,
                live ? NOTHING_LIVE_TO_UNDO_TEXT : NOTHING_TO_UNDO_TEXT,
                CheckMessages.params("action", label(language, CheckMessages::readCatalog, action), "target",
                        label(language, CheckMessages::readCatalog, target), "entity",
                        label(language, CheckMessages::readCatalog, entity)));
    }

    /**
     * The refusal of an undo of a document a reversal already reverses.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param target the generated entity
     * @param id the document's key
     * @param reversal the reversal's key
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException alreadyReversed(String catalog, Label target, Object id, Object reversal) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, ALREADY_REVERSED_KEY, ALREADY_REVERSED_TEXT,
                CheckMessages.params("target", label(language, CheckMessages::readCatalog, target), "id", id, "reversal", reversal));
    }

    /**
     * The refusal of an undo of a document in a status its reversal does not accept.
     *
     * @param catalog the intent's model catalog, or {@code null}
     * @param target the generated entity
     * @param id the document's key
     * @param allowed the labels of the statuses the reversal accepts
     * @param status the label of the document's status
     * @return the refusal, carrying the key and the parameters
     */
    public static ValidationException reversalStatus(String catalog, Label target, Object id, String allowed, String status) {
        String language = CheckMessages.language();
        return refusal(language, CheckMessages::readCatalog, catalog, REVERSAL_STATUS_KEY, REVERSAL_STATUS_TEXT, CheckMessages.params(
                "target", label(language, CheckMessages::readCatalog, target), "id", id, "allowed", allowed, "status", status));
    }

    static ValidationException reportValueMissing(String language, CheckMessages.CatalogReader catalogs, String catalog, Label entity,
            Object id, String property, String report, String parameter) {
        return refusal(language, catalogs, catalog, REPORT_VALUE_MISSING_KEY, REPORT_VALUE_MISSING_TEXT, CheckMessages.params("entity",
                label(language, catalogs, entity), "id", id, "property", property, "report", report, "parameter", parameter));
    }

    static ValidationException nothingQualifies(String language, CheckMessages.CatalogReader catalogs, String catalog, String report,
            Label action) {
        return refusal(language, catalogs, catalog, NOTHING_QUALIFIES_KEY, NOTHING_QUALIFIES_TEXT,
                CheckMessages.params("report", report, "action", label(language, catalogs, action)));
    }

    static ValidationException noItemQualifies(String language, CheckMessages.CatalogReader catalogs, String catalog, Label item,
            Label entity, Label action, Label target) {
        return refusal(language, catalogs, catalog, NO_ITEM_QUALIFIES_KEY, NO_ITEM_QUALIFIES_TEXT,
                CheckMessages.params("item", label(language, catalogs, item), "entity", label(language, catalogs, entity), "action",
                        label(language, catalogs, action), "target", label(language, catalogs, target)));
    }

    static ValidationException noReportRowQualifies(String language, CheckMessages.CatalogReader catalogs, String catalog, String report,
            Label action, Label target) {
        return refusal(language, catalogs, catalog, NO_REPORT_ROW_QUALIFIES_KEY, NO_REPORT_ROW_QUALIFIES_TEXT, CheckMessages.params(
                "report", report, "action", label(language, catalogs, action), "target", label(language, catalogs, target)));
    }

    static ValidationException itemsRefused(String language, CheckMessages.CatalogReader catalogs, String messageKey, String text,
            Label item, List<Object> rows) {
        Map<String, Object> params = CheckMessages.params("item", label(language, catalogs, item), "rows", rows);
        String refusal = CheckMessages.resolve(language, catalogs, messageKey, text, params);
        return new ValidationException(refusal + " (" + params.get("item") + " " + rows + ")", messageKey, params);
    }

    static ValidationException fromItem(String language, CheckMessages.CatalogReader catalogs, String catalog, ValidationException refused,
            Label item, Object id) {
        Map<String, Object> params =
                CheckMessages.params("message", refused.getMessage(), "item", label(language, catalogs, item), "id", id);
        String message = CheckMessages.resolve(language, catalogs, checkKey(catalog, FROM_ITEM_KEY), FROM_ITEM_TEXT, params);
        return refused instanceof DuplicateKeyException duplicate ? new DuplicateKeyException(message, duplicate)
                : new ValidationException(message, refused);
    }

    static ValidationException reportValueNotWhole(String language, CheckMessages.CatalogReader catalogs, String catalog, String column,
            Object value) {
        return refusal(language, catalogs, catalog, REPORT_VALUE_NOT_WHOLE_KEY, REPORT_VALUE_NOT_WHOLE_TEXT,
                CheckMessages.params("column", column, "value", value));
    }

    static ValidationException reportValueTooLarge(String language, CheckMessages.CatalogReader catalogs, String catalog, String column,
            Object value) {
        return refusal(language, catalogs, catalog, REPORT_VALUE_TOO_LARGE_KEY, REPORT_VALUE_TOO_LARGE_TEXT,
                CheckMessages.params("column", column, "value", value));
    }

    static ValidationException reportValueRounding(String language, CheckMessages.CatalogReader catalogs, String catalog, String column,
            Object value, int scale) {
        return refusal(language, catalogs, catalog, REPORT_VALUE_ROUNDING_KEY, REPORT_VALUE_ROUNDING_TEXT,
                CheckMessages.params("column", column, "value", value, "scale", scale));
    }

    private static ValidationException refusal(String language, CheckMessages.CatalogReader catalogs, String catalog, String key,
            String text, Map<String, Object> params) {
        String messageKey = checkKey(catalog, key);
        return new ValidationException(CheckMessages.resolve(language, catalogs, messageKey, text, params), messageKey, params);
    }

    private static String label(String language, CheckMessages.CatalogReader catalogs, Label label) {
        return CheckMessages.resolve(language, catalogs, label.key(), label.text(), Map.of());
    }

    /** The fully qualified key of a catalog's {@code checks} entry; {@code null} without a catalog. */
    private static String checkKey(String catalog, String key) {
        return catalog == null || catalog.isBlank() ? null : catalog + ".checks." + key;
    }
}
