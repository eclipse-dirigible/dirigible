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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Per-tenant document-number series store: {@code DIRIGIBLE_DOCUMENT_NUMBERS} in the tenant-routed
 * default datasource, so each tenant owns its series, shapes and counters.
 *
 * <p>
 * One row per (series, partition) - the partition being the value of the intent's {@code per}
 * relation, {@code ""} when the series is not partitioned. The row holds BOTH the shape (prefix,
 * size) and the live counter, because they are one 1:1 fact about one series; every writer touches
 * only its own columns:
 *
 * <ul>
 * <li>the {@code .numbers} synchronizer INSERTS a row that does not exist (never updates one - the
 * counter is live and the shape may have been configured);</li>
 * <li>the management surface writes {@code PREFIX} / {@code SIZE} / the counter reset;</li>
 * <li>allocation writes only {@code COUNTER}, via {@code COUNTER = COUNTER + 1}.</li>
 * </ul>
 *
 * <p>
 * Allocating from a row that does not exist FAILS - a series must be declared before a document can
 * carry a number in its shape.
 */
@Component
class DocumentNumberStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentNumberStore.class);

    private static final String TABLE_NAME = "DIRIGIBLE_DOCUMENT_NUMBERS";
    private static final String QUOTED_TABLE = "\"DIRIGIBLE_DOCUMENT_NUMBERS\"";
    private static final String COLUMN_SERIES = "DOCUMENT_SERIES";
    private static final String QUOTED_SERIES = "\"DOCUMENT_SERIES\"";
    /**
     * The partition key - the value of the intent's {@code per} relation. Keeps its original column
     * name: the column's ROLE (the counter partition) is unchanged, only what may feed it narrowed from
     * an arbitrary token map to one typed relation, so renaming it would buy nothing and cost a
     * migration.
     */
    private static final String COLUMN_PARTITION = "DOCUMENT_SCOPE";
    private static final String QUOTED_PARTITION = "\"DOCUMENT_SCOPE\"";
    private static final String COLUMN_COUNTER = "DOCUMENT_COUNTER";
    private static final String QUOTED_COUNTER = "\"DOCUMENT_COUNTER\"";
    private static final String COLUMN_PREFIX = "DOCUMENT_PREFIX";
    private static final String QUOTED_PREFIX = "\"DOCUMENT_PREFIX\"";
    private static final String COLUMN_SIZE = "DOCUMENT_SIZE";
    private static final String QUOTED_SIZE = "\"DOCUMENT_SIZE\"";

    private final DataSourcesManager dataSourcesManager;

    DocumentNumberStore(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    /**
     * One series row of the current tenant.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""} when unpartitioned)
     * @param prefix the literal prefix
     * @param size the total rendered width
     * @param counter the last allocated value
     */
    record Series(String series, String partition, String prefix, int size, long counter) {
    }

    /**
     * One allocation: the value with the shape it was allocated under, read in the same transaction so
     * a number cannot straddle a shape change.
     *
     * @param value the allocated value
     * @param prefix the prefix in force
     * @param size the width in force
     */
    record Allocation(long value, String prefix, int size) {
    }

    /**
     * Allocate the next value of (series, partition) - gap-free per tenant; a concurrent allocation
     * blocks on the increment's row lock.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""} when unpartitioned)
     * @return the allocation
     * @throws SQLException if the allocation fails
     * @throws IllegalStateException if the series is not declared for this tenant
     */
    Allocation allocate(String series, String partition) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                if (increment(connection, series, partition) == 0) {
                    // No row: the series was never declared (or not for this partition). Refusing is the
                    // point - inventing a default here would stamp a number in a shape nobody chose.
                    throw new IllegalStateException(
                            "Document-number series [" + series + "]" + (partition.isEmpty() ? "" : " partition [" + partition + "]")
                                    + " is not declared for this tenant - declare it in a .numbers artefact");
                }
                Allocation allocation = read(connection, series, partition);
                connection.commit();
                return allocation;
            } catch (SQLException | IllegalStateException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    /**
     * Every series row of the current tenant.
     *
     * @return the rows
     * @throws SQLException if the read fails
     */
    List<Series> list() throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .build();
            List<Series> result = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(new Series(resultSet.getString(COLUMN_SERIES), resultSet.getString(COLUMN_PARTITION),
                            resultSet.getString(COLUMN_PREFIX), resultSet.getInt(COLUMN_SIZE), resultSet.getLong(COLUMN_COUNTER)));
                }
            }
            return result;
        }
    }

    /**
     * Insert a declared series when this tenant has none. An existing row is left ALONE - its counter
     * is live and its shape may have been configured by an administrator; a re-publish must change
     * neither.
     *
     * @param series the series identity
     * @param partition the partition value ({@code ""} when unpartitioned)
     * @param prefix the declared default prefix
     * @param size the declared default width
     * @throws SQLException if the write fails
     */
    void provision(String series, String partition, String prefix, int size) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            if (exists(connection, series, partition)) {
                return;
            }
            String sql = SqlFactory.getNative(connection)
                                   .insert()
                                   .into(TABLE_NAME)
                                   .column(COLUMN_SERIES)
                                   .column(COLUMN_PARTITION)
                                   .column(COLUMN_COUNTER)
                                   .column(COLUMN_PREFIX)
                                   .column(COLUMN_SIZE)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, series);
                statement.setString(2, partition);
                statement.setLong(3, 0L);
                statement.setString(4, prefix == null ? "" : prefix);
                statement.setInt(5, size);
                statement.executeUpdate();
                LOGGER.info("Provisioned document-number series [{}] partition [{}] as prefix [{}] size [{}]", series, partition, prefix,
                        size);
            } catch (SQLException duplicate) {
                LOGGER.debug("Series [{}] partition [{}] was provisioned concurrently", series, partition, duplicate);
            }
        }
    }

    /**
     * Set the last-allocated value (the management surface's "next" minus one).
     *
     * @param series the series identity
     * @param partition the partition value
     * @param value the last-allocated value to store
     * @throws SQLException if the write fails
     */
    void setCounter(String series, String partition, long value) throws SQLException {
        update(series, partition, COLUMN_COUNTER, statement -> statement.setLong(1, value));
    }

    /**
     * Set the tenant's shape for a series.
     *
     * @param series the series identity
     * @param partition the partition value
     * @param prefix the literal prefix
     * @param size the total width
     * @throws SQLException if the write fails
     */
    void setShape(String series, String partition, String prefix, int size) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(TABLE_NAME)
                                   .set(COLUMN_PREFIX, "?")
                                   .set(COLUMN_SIZE, "?")
                                   .where(COLUMN_SERIES + " = ? AND " + COLUMN_PARTITION + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, prefix);
                statement.setInt(2, size);
                statement.setString(3, series);
                statement.setString(4, partition);
                statement.executeUpdate();
            }
        }
    }

    /** A single-column update of one series row. */
    private void update(String series, String partition, String column, StatementBinder binder) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            ensureTableExists(connection);
            String sql = SqlFactory.getNative(connection)
                                   .update()
                                   .table(TABLE_NAME)
                                   .set(column, "?")
                                   .where(COLUMN_SERIES + " = ? AND " + COLUMN_PARTITION + " = ?")
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                statement.setString(2, series);
                statement.setString(3, partition);
                statement.executeUpdate();
            }
        }
    }

    /** Binds the first parameter of a single-column update. */
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private boolean exists(Connection connection, String series, String partition) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column(COLUMN_SERIES)
                               .from(TABLE_NAME)
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_PARTITION + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, partition);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private int increment(Connection connection, String series, String partition) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .update()
                               .table(TABLE_NAME)
                               .set(COLUMN_COUNTER, QUOTED_COUNTER + " + 1")
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_PARTITION + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, partition);
            return statement.executeUpdate();
        }
    }

    private Allocation read(Connection connection, String series, String partition) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column(COLUMN_COUNTER)
                               .column(COLUMN_PREFIX)
                               .column(COLUMN_SIZE)
                               .from(TABLE_NAME)
                               .where(COLUMN_SERIES + " = ? AND " + COLUMN_PARTITION + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, series);
            statement.setString(2, partition);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new Allocation(resultSet.getLong(1), resultSet.getString(2), resultSet.getInt(3));
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
                               .column(QUOTED_PARTITION, DataType.VARCHAR, true, false, false, "(255)")
                               .column(QUOTED_COUNTER, DataType.BIGINT, false, false, false)
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
     * Brings a table created before the shape columns existed up to date. The table is created
     * create-if-absent and never dropped, so an existing deployment has only (series, partition,
     * counter); each column is added independently and an "already exists" failure is tolerated, so two
     * nodes racing the upgrade is harmless.
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
            LOGGER.debug("Could not add document-number column [{}]; assuming a concurrent upgrade", column, ex);
        }
    }
}
