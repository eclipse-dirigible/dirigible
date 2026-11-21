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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates entity properties from a dedicated language table - the Java counterpart of the
 * TypeScript {@code db/translator} module. A multilingual entity keeps its base values in its own
 * table and per-language overrides in a sibling <code>&lt;TABLE&gt;_LANG</code> table shaped
 * <code>GUID, Id, &lt;translated property columns&gt;, Language</code> (the codbex convention -
 * e.g. {@code CODBEX_UOM_LANG}): {@code Id} references the base row, {@code Language} is the
 * language code (e.g. {@code bg}), and every other column is named after the entity <b>property</b>
 * it overrides (e.g. {@code Name}).
 *
 * <p>
 * {@link #translateList(List, String, String)} and
 * {@link #translateEntity(Object, Object, String, String)} overlay the translated values onto
 * already-loaded entities by matching language-table columns to entity fields <b>by name,
 * case-insensitively</b> ({@code GUID}/{@code Id}/{@code
 * Language} excluded). The whole language table is read and filtered in memory - translation tables
 * are small nomenclature data, and this keeps the lookup independent of how the table's identifiers
 * were cased/quoted at creation time.
 *
 * <p>
 * A {@code null}/blank language is a no-op, and a full locale tag is reduced to its primary subtag
 * ({@code en-US} &rarr; {@code en}). A missing language table is tolerated (logged, the entities
 * stay untranslated) so a multilingual entity works before its translations arrive.
 */
public final class Translator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);

    private static final String LANGUAGE_TABLE_SUFFIX = "_LANG";
    private static final String ID_COLUMN = "Id";
    private static final String GUID_COLUMN = "GUID";
    private static final String LANGUAGE_COLUMN = "Language";

    private Translator() {}

    /**
     * Overlay the translated property values for the given language onto every entity in the list.
     *
     * @param <T> the entity type (public fields named after the model properties)
     * @param list the loaded entities; may be null or empty
     * @param language the target language code or locale tag; null/blank skips translation
     * @param table the BASE table name (the language table is {@code <table>_LANG})
     * @return the same list, with translated values applied where present
     */
    public static <T> List<T> translateList(List<T> list, String language, String table) {
        String lang = normalize(language);
        if (list == null || list.isEmpty() || lang == null) {
            return list;
        }
        List<Map<String, Object>> rows = readLanguageRows(table, lang);
        if (rows.isEmpty()) {
            return list;
        }
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = valueIgnoreCase(row, ID_COLUMN);
            if (id != null) {
                byId.put(String.valueOf(id), row);
            }
        }
        for (T entity : list) {
            Object id = readField(entity, ID_COLUMN);
            if (id == null) {
                continue;
            }
            Map<String, Object> row = byId.get(String.valueOf(id));
            if (row != null) {
                overlay(entity, row);
            }
        }
        return list;
    }

    /**
     * Overlay the translated property values for the given language onto a single entity.
     *
     * @param <T> the entity type (public fields named after the model properties)
     * @param entity the loaded entity; may be null
     * @param id the entity's identifier (matched against the language table's {@code Id})
     * @param language the target language code or locale tag; null/blank skips translation
     * @param table the BASE table name (the language table is {@code <table>_LANG})
     * @return the same entity, with translated values applied where present
     */
    public static <T> T translateEntity(T entity, Object id, String language, String table) {
        String lang = normalize(language);
        if (entity == null || id == null || lang == null) {
            return entity;
        }
        Map<String, Object> row = languageRow(table, lang, id);
        if (row != null) {
            overlay(entity, row);
        }
        return entity;
    }

    /**
     * Put back, onto an entity about to be written to the base table, the stored base value of every
     * property the read overlay replaces in the given language - the write-side counterpart of
     * {@link #translateEntity(Object, Object, String, String)}. A record read in a translated language
     * and saved whole carries the translations; without this they would overwrite the base values.
     * Exactly the properties the overlay would set are kept: the language row's non-null columns that
     * match an entity field. A language with no row for the entity, or a null/blank language, keeps
     * nothing and leaves the stored row unread.
     *
     * @param <T> the entity type (public fields named after the model properties)
     * @param entity the entity about to be updated; may be null
     * @param id the entity's identifier (matched against the language table's {@code Id})
     * @param language the language code or locale tag the write is made in; null/blank keeps nothing
     * @param table the BASE table name (the language table is {@code <table>_LANG})
     * @param stored reads the stored row, untranslated; asked only when a translation applies
     * @return the same entity, its translated properties carrying the stored base values
     */
    public static <T> T keepBaseValues(T entity, Object id, String language, String table, Supplier<T> stored) {
        String lang = normalize(language);
        if (entity == null || id == null || lang == null) {
            return entity;
        }
        Map<String, Object> row = languageRow(table, lang, id);
        if (row == null) {
            return entity;
        }
        T base = stored.get();
        if (base == null) {
            return entity;
        }
        for (Map.Entry<String, Object> column : row.entrySet()) {
            Field field = overlaidField(entity, column.getKey(), column.getValue());
            if (field == null) {
                continue;
            }
            try {
                field.set(entity, field.get(base));
            } catch (IllegalAccessException e) {
                LOGGER.warn("Cannot keep the base value of field [{}] of [{}]", field.getName(), entity.getClass()
                                                                                                       .getName(),
                        e);
            }
        }
        return entity;
    }

    /**
     * The ids of the base rows whose translation of a property, in the given language, satisfies a
     * condition - what a search in the language the user reads matches against, beside the base column.
     * The property is matched to its language-table column case-insensitively; a property the language
     * table does not carry matches nothing.
     *
     * @param table the BASE table name (the language table is {@code <table>_LANG})
     * @param language the language code or locale tag; null/blank matches nothing
     * @param property the entity property whose translated value is tested
     * @param condition the test applied to each non-null translated value
     * @return the {@code Id} values of the matching language rows, as the database returned them
     */
    public static List<Object> idsWhere(String table, String language, String property, Predicate<String> condition) {
        String lang = normalize(language);
        List<Object> ids = new ArrayList<>();
        if (lang == null) {
            return ids;
        }
        for (Map<String, Object> row : readLanguageRows(table, lang)) {
            Object translated = valueIgnoreCase(row, property);
            Object id = valueIgnoreCase(row, ID_COLUMN);
            if (translated != null && id != null && condition.test(String.valueOf(translated))) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * Reduce a locale tag to its primary language subtag, lower-cased ({@code en-US} -> {@code en});
     * null for a null/blank input.
     */
    private static String normalize(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }
        String primary = language.trim()
                                 .split("[-_,;]")[0];
        return primary.isBlank() ? null : primary.toLowerCase(Locale.ROOT);
    }

    /**
     * Read the whole language table and keep the rows of the requested language. The table name is
     * quoted (platform-created tables carry exact-case quoted identifiers); the language match is done
     * in memory, case-insensitively on both the column name and the code, so hand-created tables with
     * folded identifiers work too. A missing table is tolerated.
     */
    private static List<Map<String, Object>> readLanguageRows(String table, String language) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String languageTable = table + LANGUAGE_TABLE_SUFFIX;
        String script = "SELECT * FROM \"" + languageTable + "\"";
        try (Connection connection = Database.getConnection();
                PreparedStatement statement = connection.prepareStatement(script);
                ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columns = metaData.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columns; i++) {
                    row.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                }
                Object rowLanguage = valueIgnoreCase(row, LANGUAGE_COLUMN);
                if (rowLanguage != null && language.equalsIgnoreCase(String.valueOf(rowLanguage)
                                                                           .trim())) {
                    rows.add(row);
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Entity is marked as language dependent, but its language table [{}] is not accessible", languageTable, e);
        }
        return rows;
    }

    /**
     * Set every language-row column onto the matching entity field (by name, case-insensitively),
     * skipping the {@code GUID}/{@code Id}/{@code Language} bookkeeping columns, null values, and
     * columns without a matching field.
     */
    private static void overlay(Object entity, Map<String, Object> row) {
        for (Map.Entry<String, Object> column : row.entrySet()) {
            Object value = column.getValue();
            Field field = overlaidField(entity, column.getKey(), value);
            if (field == null) {
                continue;
            }
            try {
                field.set(entity, field.getType()
                                       .isAssignableFrom(value.getClass()) ? value : String.valueOf(value));
            } catch (IllegalAccessException e) {
                LOGGER.warn("Cannot overlay translated value onto field [{}] of [{}]", field.getName(), entity.getClass()
                                                                                                              .getName(),
                        e);
            }
        }
    }

    /**
     * The entity field a language-row column overlays, or null when the column overlays nothing: a
     * bookkeeping column, a null value, a column without a matching field, or a value the field can
     * take neither as it is nor as a string.
     */
    private static Field overlaidField(Object entity, String column, Object value) {
        if (value == null || GUID_COLUMN.equalsIgnoreCase(column) || ID_COLUMN.equalsIgnoreCase(column)
                || LANGUAGE_COLUMN.equalsIgnoreCase(column)) {
            return null;
        }
        Field field = fieldIgnoreCase(entity.getClass(), column);
        if (field == null || !(field.getType()
                                    .isAssignableFrom(value.getClass())
                || field.getType() == String.class)) {
            return null;
        }
        return field;
    }

    /** The language row of the given entity id, or null when the language has none. */
    private static Map<String, Object> languageRow(String table, String language, Object id) {
        for (Map<String, Object> row : readLanguageRows(table, language)) {
            Object rowId = valueIgnoreCase(row, ID_COLUMN);
            if (rowId != null && String.valueOf(rowId)
                                       .equals(String.valueOf(id))) {
                return row;
            }
        }
        return null;
    }

    private static Object readField(Object entity, String name) {
        Field field = fieldIgnoreCase(entity.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            LOGGER.warn("Cannot read field [{}] of [{}]", name, entity.getClass()
                                                                      .getName(),
                    e);
            return null;
        }
    }

    private static Field fieldIgnoreCase(Class<?> type, String name) {
        for (Field field : type.getFields()) {
            if (field.getName()
                     .equalsIgnoreCase(name)) {
                return field;
            }
        }
        return null;
    }

    private static Object valueIgnoreCase(Map<String, Object> row, String column) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey()
                     .equalsIgnoreCase(column)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
