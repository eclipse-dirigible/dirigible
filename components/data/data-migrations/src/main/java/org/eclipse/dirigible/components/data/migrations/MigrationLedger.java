/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The {@code DIRIGIBLE_MIGRATIONS} ledger: one row per migration applied to the database it lives
 * in. It lives in the database the migrations change - every tenant schema has its own, the system
 * database has one for the {@code tenant: system} migrations - so a migration and the row recording
 * it commit in one transaction, and a restored backup carries the ledger that matches its data. The
 * table is created on first use.
 *
 * <p>
 * The primary key is {@code <project>/<version>}: two nodes applying the same migration at once
 * cannot both record it, and the loser's insert fails its transaction, rolling its run back with
 * it.
 */
@Component
class MigrationLedger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationLedger.class);

    /** Unquoted table name - used for metadata existence checks and by the DML builders. */
    static final String TABLE_NAME = "DIRIGIBLE_MIGRATIONS";

    private static final String COLUMN_KEY = "MIGRATION_KEY";
    private static final String COLUMN_PROJECT = "MIGRATION_PROJECT";
    private static final String COLUMN_VERSION = "MIGRATION_VERSION";
    private static final String COLUMN_LOCATION = "MIGRATION_LOCATION";
    private static final String COLUMN_CHECKSUM = "MIGRATION_CHECKSUM";
    private static final String COLUMN_TENANT = "MIGRATION_TENANT";
    private static final String COLUMN_APPLIED_AT = "MIGRATION_APPLIED_AT";
    private static final String COLUMN_DURATION = "MIGRATION_DURATION_MILLIS";

    /**
     * One applied migration.
     *
     * @param project the project the migration belongs to
     * @param version the migration's version
     * @param location the registry-relative location of the file that was applied
     * @param checksum the checksum of the content that was applied
     * @param tenant the tenant whose database the migration was applied to, {@code system} for the
     *        system database
     * @param appliedAt when the migration was (last) applied
     * @param durationMillis how long the run took
     */
    record Entry(String project, String version, String location, String checksum, String tenant, Instant appliedAt, long durationMillis) {

        String key() {
            return project + "/" + version;
        }
    }

    /**
     * Makes sure the ledger exists in the given database, on a connection of its own so that no DDL
     * runs inside a migration's transaction.
     *
     * @param dataSource the database
     * @throws SQLException if the table is missing and cannot be created
     */
    void prepare(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, TABLE_NAME)) {
                return;
            }
            String sql = SqlFactory.getNative(connection)
                                   .create()
                                   .table(quoted(TABLE_NAME))
                                   .column(quoted(COLUMN_KEY), DataType.VARCHAR, true, false, false, "(512)")
                                   .column(quoted(COLUMN_PROJECT), DataType.VARCHAR, false, false, false, "(255)")
                                   .column(quoted(COLUMN_VERSION), DataType.VARCHAR, false, false, false, "(64)")
                                   .column(quoted(COLUMN_LOCATION), DataType.VARCHAR, false, false, false, "(1024)")
                                   .column(quoted(COLUMN_CHECKSUM), DataType.VARCHAR, false, false, false, "(64)")
                                   .column(quoted(COLUMN_TENANT), DataType.VARCHAR, false, false, false, "(255)")
                                   .column(quoted(COLUMN_APPLIED_AT), DataType.TIMESTAMP, false, false, false)
                                   .column(quoted(COLUMN_DURATION), DataType.BIGINT, false, false, false)
                                   .build();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.executeUpdate();
                LOGGER.info("Created the migrations ledger [{}]", TABLE_NAME);
            } catch (SQLException ex) {
                // Another node or tenant pass may have created it in the meantime; tolerate it.
                if (SqlFactory.getNative(connection)
                              .existsTable(connection, TABLE_NAME)) {
                    LOGGER.debug("The migrations ledger already exists after a concurrent creation", ex);
                    return;
                }
                throw ex;
            }
        }
    }

    /**
     * Reads the applied migrations of a project, keyed by version.
     *
     * @param connection a connection to the database the ledger lives in
     * @param project the project
     * @return the project's applied migrations
     * @throws SQLException if the read fails
     */
    Map<String, Entry> findByProject(Connection connection, String project) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .select()
                               .column("*")
                               .from(TABLE_NAME)
                               .where(COLUMN_PROJECT + " = ?")
                               .build();
        Map<String, Entry> applied = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, project);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Entry entry = read(resultSet);
                    applied.put(entry.version(), entry);
                }
            }
        }
        return applied;
    }

    /**
     * Records a migration applied for the first time.
     *
     * @param connection the connection of the transaction that ran the migration
     * @param entry the applied migration
     * @throws SQLException if the insert fails - also when another node recorded it first
     */
    void insert(Connection connection, Entry entry) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .insert()
                               .into(TABLE_NAME)
                               .column(COLUMN_KEY)
                               .column(COLUMN_PROJECT)
                               .column(COLUMN_VERSION)
                               .column(COLUMN_LOCATION)
                               .column(COLUMN_CHECKSUM)
                               .column(COLUMN_TENANT)
                               .column(COLUMN_APPLIED_AT)
                               .column(COLUMN_DURATION)
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.key());
            statement.setString(2, entry.project());
            statement.setString(3, entry.version());
            statement.setString(4, entry.location());
            statement.setString(5, entry.checksum());
            statement.setString(6, entry.tenant());
            statement.setTimestamp(7, Timestamp.from(entry.appliedAt()));
            statement.setLong(8, entry.durationMillis());
            statement.executeUpdate();
        }
    }

    /**
     * Records the re-application of an idempotent migration whose content changed.
     *
     * @param connection the connection of the transaction that re-ran the migration
     * @param entry the re-applied migration
     * @throws SQLException if the update fails
     */
    void update(Connection connection, Entry entry) throws SQLException {
        String sql = SqlFactory.getNative(connection)
                               .update()
                               .table(TABLE_NAME)
                               .set(COLUMN_LOCATION, "?")
                               .set(COLUMN_CHECKSUM, "?")
                               .set(COLUMN_APPLIED_AT, "?")
                               .set(COLUMN_DURATION, "?")
                               .where(COLUMN_KEY + " = ?")
                               .build();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.location());
            statement.setString(2, entry.checksum());
            statement.setTimestamp(3, Timestamp.from(entry.appliedAt()));
            statement.setLong(4, entry.durationMillis());
            statement.setString(5, entry.key());
            statement.executeUpdate();
        }
    }

    /**
     * Reads every applied migration of a database, by project and version.
     *
     * @param dataSource the database
     * @return the applied migrations, empty when the database has no ledger yet
     * @throws SQLException if the read fails
     */
    List<Entry> findAll(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!SqlFactory.getNative(connection)
                           .existsTable(connection, TABLE_NAME)) {
                return List.of();
            }
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("*")
                                   .from(TABLE_NAME)
                                   .order(COLUMN_PROJECT)
                                   .order(COLUMN_APPLIED_AT)
                                   .build();
            List<Entry> entries = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(read(resultSet));
                }
            }
            return entries;
        }
    }

    private static Entry read(ResultSet resultSet) throws SQLException {
        return new Entry(resultSet.getString(COLUMN_PROJECT), resultSet.getString(COLUMN_VERSION), resultSet.getString(COLUMN_LOCATION),
                resultSet.getString(COLUMN_CHECKSUM), resultSet.getString(COLUMN_TENANT), resultSet.getTimestamp(COLUMN_APPLIED_AT)
                                                                                                   .toInstant(),
                resultSet.getLong(COLUMN_DURATION));
    }

    private static String quoted(String identifier) {
        return "\"" + identifier + "\"";
    }
}
