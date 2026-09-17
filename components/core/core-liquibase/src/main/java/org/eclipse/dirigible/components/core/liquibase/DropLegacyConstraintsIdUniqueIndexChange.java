/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.core.liquibase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

/**
 * Drops the legacy unique index a pre-changelog {@code hbm2ddl} bootstrap put on the
 * {@code CONSTRAINTS_ID} join column of {@code DIRIGIBLE_DATA_TABLE_UNIQUES} / {@code _FOREIGNKEYS}
 * / {@code _CHECKS} (see the {@code @OneToOne}-to-{@code @ManyToOne} change in #7343). The
 * changelog-named copy is dropped by name by the {@code dropUniqueConstraint} changesets ahead of
 * this one; the Hibernate-named copy has no known name, so it must be found through
 * {@code information_schema} and dropped by its actual name.
 *
 * <p>
 * This supersedes the raw-SQL {@code drop-legacy-named-UK_DIRIGIBLE_DATA_TABLE_CONSTRAINTS_ID-h2} /
 * {@code -postgresql} sweeps shipped in 14.56.0 (#7343), which had two defects (#7370): they read
 * {@code information_schema} without a schema filter and dropped without qualifying the table, so
 * on a database that holds those tables in more than one schema the lookup and the
 * {@code ALTER TABLE} could address different schemas - dropping the wrong constraint or failing
 * the changeset - and the H2 variant collapsed its match to {@code MAX(...)}, dropping at most one
 * constraint per table and silently leaving any second one. Both released sweeps are immutable
 * (their checksums are recorded on every deployed instance), so the correction is this new,
 * checksum-independent changeset rather than an edit to them.
 *
 * <p>
 * A {@link CustomTaskChange} rather than dialect SQL because the operation cannot be expressed
 * portably in the changelog: PostgreSQL needs a {@code DO} loop to drop more than one, and H2 has
 * no procedural SQL at all ({@code EXECUTE IMMEDIATE} runs a single statement and
 * {@code ALTER TABLE} takes a single {@code DROP CONSTRAINT}), so neither can both schema-qualify
 * AND drop every match. JDBC does both, once, for both databases. It queries the current schema
 * only and drops every matching constraint it finds there, qualifying each {@code ALTER TABLE} with
 * that schema; a schema with no such constraint yields no rows and the change is a no-op, so it
 * stays idempotent. MSSQL is out of scope exactly as the original sweeps were (no CI leg verifies
 * it) - the changeset is gated to H2 and PostgreSQL by its preconditions.
 */
public class DropLegacyConstraintsIdUniqueIndexChange implements CustomTaskChange {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropLegacyConstraintsIdUniqueIndexChange.class);

    /**
     * Every {@code UNIQUE} constraint on the {@code CONSTRAINTS_ID} column of the three constraint
     * tables, restricted to the connection's current schema and joined within that same schema so a
     * same-named constraint in another schema can never be matched. {@code UPPER(...)} on the table and
     * column names covers both the upper-case identifiers H2 stores and the lower-case ones PostgreSQL
     * stores.
     */
    private static final String SELECT_LEGACY_CONSTRAINTS = """
            SELECT tc.table_schema, tc.table_name, tc.constraint_name
              FROM information_schema.table_constraints tc
              JOIN information_schema.key_column_usage k
                ON k.constraint_schema = tc.constraint_schema
               AND k.constraint_name = tc.constraint_name
               AND k.table_name = tc.table_name
             WHERE tc.constraint_type = 'UNIQUE'
               AND tc.table_schema = ?
               AND UPPER(tc.table_name) IN ('DIRIGIBLE_DATA_TABLE_UNIQUES', 'DIRIGIBLE_DATA_TABLE_FOREIGNKEYS',
                                            'DIRIGIBLE_DATA_TABLE_CHECKS')
               AND UPPER(k.column_name) = 'CONSTRAINTS_ID'
            """;

    @Override
    public void execute(Database database) throws CustomChangeException {
        Connection connection = ((JdbcConnection) database.getConnection()).getWrappedConnection();
        try {
            String schema = connection.getSchema();
            if (schema == null || schema.isBlank()) {
                schema = database.getDefaultSchemaName();
            }
            List<String[]> constraints = findLegacyConstraints(connection, schema);
            for (String[] constraint : constraints) {
                dropConstraint(connection, constraint[0], constraint[1], constraint[2]);
            }
        } catch (SQLException e) {
            throw new CustomChangeException("Failed to drop the legacy CONSTRAINTS_ID unique constraint(s)", e);
        }
    }

    private static List<String[]> findLegacyConstraints(Connection connection, String schema) throws SQLException {
        List<String[]> constraints = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_LEGACY_CONSTRAINTS)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    constraints.add(new String[] {resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)});
                }
            }
        }
        return constraints;
    }

    private static void dropConstraint(Connection connection, String schema, String table, String constraint) throws SQLException {
        String ddl = "ALTER TABLE " + quote(schema) + "." + quote(table) + " DROP CONSTRAINT " + quote(constraint);
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
        }
        LOGGER.info("Dropped legacy unique constraint [{}] on [{}].[{}]", constraint, schema, table);
    }

    /**
     * Delimits an identifier taken from {@code information_schema} so the {@code ALTER TABLE} names it
     * exactly as it is stored, whatever its case; an embedded double quote is doubled per the SQL rule.
     *
     * @param identifier the raw identifier
     * @return the double-quoted identifier
     */
    private static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    @Override
    public String getConfirmationMessage() {
        return "Dropped the legacy CONSTRAINTS_ID unique constraint(s) in the current schema";
    }

    @Override
    public void setUp() throws SetupException {
        // no set-up needed
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // no resources needed
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
