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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.sdk.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Appends and reads the change history of an entity kept in a shadow
 * <code>&lt;TABLE&gt;_HISTORY</code> table - the sibling-table counterpart of {@link Translator}'s
 * <code>&lt;TABLE&gt;_LANG</code>. Where the four audit columns record only the LAST writer and
 * time in the row itself, the history records <b>every</b> write as field-level deltas: one row per
 * property whose value changed, shaped
 * <code>GUID, Id, Operation, Property, OldValue, NewValue, ChangedAt, ChangedBy, Source</code>.
 *
 * <p>
 * A create is recorded as {@code null -> value} and a delete as {@code value -> null}, so the three
 * operations are one uniform diff and the trail alone reconstructs the row at any point in time.
 * {@code Source} separates a user edit from a system write (a roll-up total, a workflow
 * write-back), which are indistinguishable once they land in the same column.
 *
 * <p>
 * The generated entity repository is the only writer - there is no update or delete here, and no
 * endpoint offers one, so the table is append-only by construction. Values are stringified for
 * storage (a history row is read, never joined) and truncated at the column width.
 *
 * <p>
 * The append happens after the entity write has committed, on its own connection: the platform's
 * store commits each operation in its own transaction, so there is no enclosing transaction to
 * join. A failure to append is therefore logged at ERROR and does not fail the business write,
 * which has already happened - reporting failure for a change that was persisted would be the worse
 * lie.
 */
public final class History {

    /** {@code Source} value for a write made by a user through the entity's normal write path. */
    public static final String USER = "USER";

    /**
     * {@code Source} value for a write made by the system - a roll-up or aggregate total, a workflow
     * write-back, any targeted single-column write.
     */
    public static final String SYSTEM = "SYSTEM";

    private static final Logger LOGGER = LoggerFactory.getLogger(History.class);

    private static final String HISTORY_TABLE_SUFFIX = "_HISTORY";
    private static final String CREATE = "CREATE";
    private static final String UPDATE = "UPDATE";
    private static final String DELETE = "DELETE";
    private static final int VALUE_LENGTH = 4000;

    private History() {}

    /**
     * Record the creation of a row: every property carrying a value is one {@code null -> value} row.
     *
     * @param table the BASE table name (the history table is {@code <table>_HISTORY})
     * @param id the created row's identifier
     * @param source {@link #USER} or {@link #SYSTEM}
     * @param created the persisted entity
     * @param properties the entity properties to track (public fields named after the model properties)
     */
    public static void recordCreate(String table, Object id, String source, Object created, List<String> properties) {
        record(table, id, CREATE, source, null, created, properties);
    }

    /**
     * Record an update: one row per property whose value actually changed. An unchanged property writes
     * nothing, so a partial payload or a recomputation that lands on the same value leaves no trace.
     *
     * @param table the BASE table name (the history table is {@code <table>_HISTORY})
     * @param id the row's identifier
     * @param source {@link #USER} or {@link #SYSTEM}
     * @param before the stored row as it was before the write; {@code null} records nothing
     * @param after the persisted entity
     * @param properties the entity properties to track (public fields named after the model properties)
     */
    public static void recordUpdate(String table, Object id, String source, Object before, Object after, List<String> properties) {
        if (before == null) {
            return;
        }
        record(table, id, UPDATE, source, before, after, properties);
    }

    /**
     * Record the deletion of a row: every property carrying a value is one {@code value -> null} row,
     * so the deleted row's last state stays readable in the trail.
     *
     * @param table the BASE table name (the history table is {@code <table>_HISTORY})
     * @param id the deleted row's identifier
     * @param source {@link #USER} or {@link #SYSTEM}
     * @param deleted the entity as it was before the delete
     * @param properties the entity properties to track (public fields named after the model properties)
     */
    public static void recordDelete(String table, Object id, String source, Object deleted, List<String> properties) {
        record(table, id, DELETE, source, deleted, null, properties);
    }

    /**
     * Read a row's change history, newest first.
     *
     * @param table the BASE table name (the history table is {@code <table>_HISTORY})
     * @param id the row's identifier
     * @return the history entries as maps keyed by column name; empty when there is nothing recorded
     *         (or the table is not accessible yet)
     */
    public static List<Map<String, Object>> read(String table, Object id) {
        List<Map<String, Object>> entries = new ArrayList<>();
        if (id == null) {
            return entries;
        }
        String historyTable = table + HISTORY_TABLE_SUFFIX;
        String script = "SELECT * FROM \"" + historyTable + "\" WHERE \"Id\" = ? ORDER BY \"ChangedAt\" DESC, \"GUID\" DESC";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(script)) {
            statement.setObject(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columns = metaData.getColumnCount();
                while (resultSet.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    for (int i = 1; i <= columns; i++) {
                        entry.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                    }
                    entries.add(entry);
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Cannot read the change history of [{}] row [{}] from [{}]", table, id, historyTable, e);
        }
        return entries;
    }

    /**
     * Append one row per changed property. {@code before}/{@code after} may be null (create / delete);
     * a property equal on both sides writes nothing.
     */
    private static void record(String table, Object id, String operation, String source, Object before, Object after,
            List<String> properties) {
        if (id == null || properties == null || properties.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (String property : properties) {
            Object oldValue = readField(before, property);
            Object newValue = readField(after, property);
            if (unchanged(oldValue, newValue)) {
                continue;
            }
            rows.add(new Object[] {property, text(oldValue), text(newValue)});
        }
        if (rows.isEmpty()) {
            return;
        }
        String historyTable = table + HISTORY_TABLE_SUFFIX;
        String script = "INSERT INTO \"" + historyTable
                + "\" (\"Id\", \"Operation\", \"Property\", \"OldValue\", \"NewValue\", \"ChangedAt\", \"ChangedBy\", \"Source\")"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Timestamp changedAt = Timestamp.from(Instant.now());
        String changedBy = currentUser();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(script)) {
            for (Object[] row : rows) {
                statement.setObject(1, id);
                statement.setString(2, operation);
                statement.setString(3, (String) row[0]);
                statement.setString(4, (String) row[1]);
                statement.setString(5, (String) row[2]);
                statement.setTimestamp(6, changedAt);
                statement.setString(7, changedBy);
                statement.setString(8, source);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Throwable e) {
            LOGGER.error("Cannot record the {} of [{}] row [{}] in [{}]", operation, table, id, historyTable, e);
        }
    }

    /**
     * Whether the two values are the same change-wise. Decimals are compared by VALUE, not by
     * {@code equals}: a recomputed total of {@code 2.0} and a stored {@code 2.00} are the same amount,
     * and treating them as a change would fill the trail with edits nobody made.
     */
    private static boolean unchanged(Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null) {
            return oldValue == null && newValue == null;
        }
        if (oldValue instanceof BigDecimal && newValue instanceof BigDecimal) {
            return ((BigDecimal) oldValue).compareTo((BigDecimal) newValue) == 0;
        }
        return oldValue.equals(newValue);
    }

    /** The stored form of a value: its string, truncated to the column width; null stays null. */
    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() > VALUE_LENGTH ? text.substring(0, VALUE_LENGTH) : text;
    }

    private static Object readField(Object entity, String name) {
        if (entity == null) {
            return null;
        }
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

    /**
     * The acting login - the same identity the audit columns stamp, so "who" means the same thing in
     * both. Never let an unresolvable user cost the whole history row.
     */
    private static String currentUser() {
        try {
            return User.getName();
        } catch (Throwable e) {
            LOGGER.warn("Cannot resolve the acting user for a history entry", e);
            return null;
        }
    }
}
