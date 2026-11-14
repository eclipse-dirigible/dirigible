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
 * value left out, a text longer than its column, a value off its pattern, a value another record
 * already holds under a {@code unique: true} - their messages resolved for the language of the
 * request exactly as a generated check's are, see {@link CheckMessages} (dirigible #8596).
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

    /** The key, under a catalog's {@code checks}, of the sentence refusing a repeated unique value. */
    public static final String DUPLICATE_KEY = "fieldDuplicate";

    /**
     * The default-language sentence refusing a repeated unique value: {@code {entity}} is the entity's.
     */
    public static final String DUPLICATE_TEXT = "A {entity} with this '{label}' already exists";

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
