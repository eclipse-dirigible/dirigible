/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Per-tenant document-number counter store. Backed by {@code DIRIGIBLE_DOCUMENT_NUMBERS} in the
 * tenant-routed default datasource (so each tenant gets its own counters), keyed by (series,
 * scope). {@link #allocate(String, String)} returns the next value gap-free; a concurrent
 * allocation of the same (series, scope) serializes on the row lock the increment takes.
 */
@Component
class DocumentNumberStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentNumberStore.class);

    private static final String TABLE_NAME = "DIRIGIBLE_DOCUMENT_NUMBERS";
    private static final String QUOTED_TABLE = "\"DIRIGIBLE_DOCUMENT_NUMBERS\"";
    private static final String COLUMN_SERIES = "DOCUMENT_SERIES";
    private static final String QUOTED_SERIES = "\"DOCUMENT_SERIES\"";
    private static final String COLUMN_SCOPE = "DOCUMENT_SCOPE";
    private static final String QUOTED_SCOPE = "\"DOCUMENT_SCOPE\"";
    private static final String COLUMN_COUNTER = "DOCUMENT_COUNTER";
    private static final String QUOTED_COUNTER = "\"DOCUMENT_COUNTER\"";
    /** The AUTHORED format, recorded by declare() so the management surface knows the series shape. */
    private static final String COLUMN_FORMAT = "DOCUMENT_FORMAT";
    private static final String QUOTED_FORMAT = "\"DOCUMENT_FORMAT\"";
    /** Per-tenant overrides of the authored format: a literal prefix and the total rendered width. */
    private static final String COLUMN_PREFIX = "DOCUMENT_PREFIX";
    private static final String QUOTED_PREFIX = "\"DOCUMENT_PREFIX\"";
    private static final String COLUMN_SIZE = "DOCUMENT_SIZE";
    private static final String QUOTED_SIZE = "\"DOCUMENT_SIZE\"";

    private final DataSourcesManager dataSourcesManager;

    DocumentNumberStore(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * A counter row - the current value of one (series, scope) counter in the current tenant.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""} for an unscoped series)
     * @param counter the current (last allocated) value
     */
    record Counter(String series, String scope, long counter, String format, String prefix, Integer size) {
    }

    /**
     * One allocation: the value plus the tenant's format override, read inside the same transaction so
     * a number is rendered with the configuration it was allocated under.
     *
     * @param seq the allocated value
     * @param prefix the tenant's literal prefix override, or null
     * @param size the tenant's total-width override, or null
     */
    record Allocation(long seq, String prefix, Integer size) {
    }

    /**
     * Allocate the next value for (series, scope) - gap-free per tenant. Creates the counter row on
     * first use (starting at 1); a concurrent allocation blocks on the increment's row lock.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""} for unscoped)
     * @return the newly allocated value
     * @throws SQLException if the allocation fails
     */
    Allocation allocate(String series, String scope) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (increment(connection, series, scope) == 0) {
                    try {
                        insert(connection, series, scope, 1L);
                    } catch (SQLException duplicate) {
                        // A concurrent first allocation created the row; increment the now-present row.
                        LOGGER.debug("Concurrent counter creation for series [{}] scope [{}]; retrying increment", series, scope,
                                duplicate);
                        increment(connection, series, scope);
                    }
                }
                Allocation allocation = readAllocation(connection, series, scope);
                connection.commit();
                return allocation;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    /** All counter rows of the current tenant, insertion-ordered. */
    List<Counter> list() throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .build();
            List<Counter> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int size = resultSet.getInt(COLUMN_SIZE);
                    result.add(new Counter(resultSet.getString(COLUMN_SERIES), resultSet.getString(COLUMN_SCOPE),
                            resultSet.getLong(COLUMN_COUNTER), resultSet.getString(COLUMN_FORMAT), resultSet.getString(COLUMN_PREFIX),
                            resultSet.wasNull() || size == 0 ? null : Integer.valueOf(size)));
                }
            }
            return result;
        }
    }

    /**
     * Set the current value of a (series, scope) counter (the next allocation returns
     * {@code value + 1}). Creates the row if absent. Used by the management surface to reset/seed a
     * counter.
     */
    void setCounter(String series, String scope, long value) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String update = SqlFactory.getNative(connection)
                                      .update()
                                      .table(TABLE_NAME)
                                      .set(COLUMN_COUNTER, "?")
                                      .where(COLUMN_SERIES + " = ? AND " + COLUMN_SCOPE + " = ?")
                                      .build();
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setLong(1, value);
                statement.setString(2, series);
                statement.setString(3, scope);
                if (statement.executeUpdate() == 0) {
                    insert(connection, series, scope, 1L);
                    setCounter(series, scope, value);
                }
            }
        }
    }

    private int increment(Connection connection, String series, String scope) throws SQLException {
        // The table is created with QUOTED (case-sensitive) identifiers, and the builder encapsulates
        // the SET-target column and the WHERE-condition identifiers the same way. But the SET VALUE is a
        // free expression appended verbatim (NOT encapsulated) - so its column reference must be quoted
        // explicitly, otherwise a case-folding dialect (PostgreSQL lower-cases unquoted identifiers)
        // fails to resolve it against the quoted-uppercase column (H2 upper-cases unquoted, so it
        // happened to match). The WHERE columns stay unquoted: the builder quotes those for us (quoting
        // them here would double-encapsulate into a zero-length delimited identifier).
        String sql = SqlFactory.getNative(connection)
                               .update()
                               .table(TABLE_NAME)
                               .set(COLUMN_COUNTER, QUOTED_COUNTER + " + 1")
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_SCOPE + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, scope);
            return statement.executeUpdate();
        }
    }

    private void insert(Connection connection, String series, String scope, long counter) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .insert()
                               .into(TABLE_NAME)
                               .column(COLUMN_SERIES)
                               .column(COLUMN_SCOPE)
                               .column(COLUMN_COUNTER)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, scope);
            statement.setLong(3, counter);
            statement.executeUpdate();
        }
    }

    private long read(Connection connection, String series, String scope) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column(COLUMN_COUNTER)
                               .from(TABLE_NAME)
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_SCOPE + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, scope);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        }
    }

    private void ensureTableExists(Connection connection) throws SQLException {
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, TABLE_NAME)) {
            addMissingColumns(connection);
            return;
        }
        String sql = SqlFactory.getNative(connection)
                               .create()
                               .table(QUOTED_TABLE)
                               .column(QUOTED_SERIES, DataType.VARCHAR, true, false, false, "(255)")
                               .column(QUOTED_SCOPE, DataType.VARCHAR, true, false, false, "(255)")
                               .column(QUOTED_COUNTER, DataType.BIGINT, false, false, false)
                               .column(QUOTED_FORMAT, DataType.VARCHAR, false, true, false, "(255)")
                               .column(QUOTED_PREFIX, DataType.VARCHAR, false, true, false, "(64)")
                               .column(QUOTED_SIZE, DataType.INTEGER, false, true, false)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Created per-tenant document-number table using sql [{}]", sql);
        } catch (SQLException ex) {
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, TABLE_NAME)) {
                LOGGER.debug("Document-number table already created concurrently", ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Brings a table created by an earlier version up to the current shape. The counter table predates
     * the declared format and the per-tenant prefix/width overrides, and it is created create-if-absent
     * (never dropped), so an existing deployment would otherwise keep a three-column table and every
     * read of the new columns would fail. Each column is added independently and an "already exists"
     * failure is tolerated, so two nodes racing the same upgrade is harmless.
     *
     * @param connection the connection
     * @throws SQLException if the table cannot be inspected
     */
    private void addMissingColumns(Connection connection) throws SQLException {
        Set<String> present = new HashSet<>();
        try (ResultSet columns = connection.getMetaData()
                                           .getColumns(null, null, TABLE_NAME, null)) {
            while (columns.next()) {
                present.add(columns.getString("COLUMN_NAME")
                                   .toUpperCase(Locale.ROOT));
            }
        }
        addColumnIfMissing(connection, present, QUOTED_FORMAT, COLUMN_FORMAT, "VARCHAR(255)");
        addColumnIfMissing(connection, present, QUOTED_PREFIX, COLUMN_PREFIX, "VARCHAR(64)");
        addColumnIfMissing(connection, present, QUOTED_SIZE, COLUMN_SIZE, "INTEGER");
    }

    private void addColumnIfMissing(Connection connection, Set<String> present, String quotedColumn, String column, String type) {
        if (present.contains(column)) {
            return;
        }
        String sql = "ALTER TABLE " + QUOTED_TABLE + " ADD COLUMN " + quotedColumn + " " + type;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.info("Added document-number column [{}] using sql [{}]", column, sql);
        } catch (SQLException ex) {
            // A concurrent node added it first - the next read proves whether that is what happened.
            LOGGER.debug("Could not add document-number column [{}]; assuming a concurrent upgrade", column, ex);
        }
    }

    /**
     * Sets (or clears) the tenant's format override for a series.
     *
     * @param series the series identity
     * @param scope the scope key ({@code ""} for unscoped)
     * @param prefix the literal prefix, or null to clear the override
     * @param size the total rendered width, or null to clear the override
     * @throws SQLException if the write fails
     */
    void setOverride(String series, String scope, String prefix, Integer size) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(TABLE_NAME)
                                   .set(COLUMN_PREFIX, "?")
                                   .set(COLUMN_SIZE, "?")
                                   .where(COLUMN_SERIES + " = ? AND " + COLUMN_SCOPE + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, prefix);
                if (size == null) {
                    statement.setNull(2, java.sql.Types.INTEGER);
                } else {
                    statement.setInt(2, size);
                }
                statement.setString(3, series);
                statement.setString(4, scope);
                if (statement.executeUpdate() == 0) {
                    // Declared but never allocated: counter 0 keeps the first number at 1.
                    insert(connection, series, scope, 0L);
                    setOverride(series, scope, prefix, size);
                }
            }
        }
    }

    /**
     * The freshly allocated value together with the tenant's format override, read in the allocating
     * transaction so the rendered number cannot straddle a configuration change.
     *
     * @param connection the connection
     * @param series the series identity
     * @param scope the scope key
     * @return the allocation
     * @throws SQLException if the read fails
     */
    private Allocation readAllocation(Connection connection, String series, String scope) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column(COLUMN_COUNTER)
                               .column(COLUMN_PREFIX)
                               .column(COLUMN_SIZE)
                               .from(TABLE_NAME)
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_SCOPE + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, scope);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return new Allocation(0L, null, null);
                }
                long counter = resultSet.getLong(1);
                String prefix = resultSet.getString(2);
                int size = resultSet.getInt(3);
                return new Allocation(counter, prefix, resultSet.wasNull() || size == 0 ? null : Integer.valueOf(size));
            }
        }
    }
}
