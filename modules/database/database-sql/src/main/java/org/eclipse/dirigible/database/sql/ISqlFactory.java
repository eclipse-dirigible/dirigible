/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.ExpressionBuilder;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.NextValueSequenceBuilder;

/**
 * A factory for creating ISql objects.
 *
 * @param <SELECT> the generic type
 * @param <INSERT> the generic type
 * @param <UPDATE> the generic type
 * @param <DELETE> the generic type
 * @param <CREATE> the generic type
 * @param <ALTER> the generic type
 * @param <DROP> the generic type
 * @param <NEXT> the generic type
 * @param <LAST> the generic type
 */
public interface ISqlFactory<SELECT extends SelectBuilder, INSERT extends InsertBuilder, UPDATE extends UpdateBuilder, DELETE extends DeleteBuilder, CREATE extends CreateBranchingBuilder, ALTER extends AlterBranchingBuilder, DROP extends DropBranchingBuilder, NEXT extends NextValueSequenceBuilder, LAST extends LastValueIdentityBuilder> {

    /**
     * Select.
     *
     * @return the select
     */
    public SELECT select();

    /**
     * Insert.
     *
     * @return the insert
     */
    public INSERT insert();

    /**
     * Update.
     *
     * @return the update
     */
    public UPDATE update();

    /**
     * Delete.
     *
     * @return the delete
     */
    public DELETE delete();

    /**
     * Expression.
     *
     * @return the expression builder
     */
    public ExpressionBuilder expression();

    /**
     * Creates the.
     *
     * @return the creates the
     */
    public CREATE create();

    /**
     * Alters the.
     *
     * @return the alters the
     */
    public ALTER alter();

    /**
     * Drop.
     *
     * @return the drop
     */
    public DROP drop();

    /**
     * Exists.
     *
     * @param connection the connection
     * @param table the table
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    public boolean existsTable(Connection connection, String table) throws SQLException;

    /**
     * Check existence of an artifacts.
     *
     * @param connection the current connection
     * @param name the artifact name
     * @param type the artifact type
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */
    public boolean exists(Connection connection, String name, int type) throws SQLException;

    /**
     * Check existence of an artifacts.
     *
     * @param connection the current connection
     * @param schema the schema name
     * @param name the artifact name
     * @param type the artifact type
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */
    public boolean exists(Connection connection, String schema, String name, int type) throws SQLException;

    /**
     * Check existence of a schema.
     *
     * @param connection the current connection
     * @param schema the schema name
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */
    public boolean existsSchema(Connection connection, String schema) throws SQLException;

    /**
     * The UNIQUE constraints of a table as the database catalog reports them - never the primary key,
     * never a foreign key: constraint name to its columns in ordinal order.
     *
     * @param connection the current connection
     * @param table the (unquoted) table name
     * @return constraint name to ordered column names, empty when the table has none; {@code null} when
     *         this database exposes no catalog of unique constraints at all
     * @throws SQLException when the catalog read fails
     */
    Map<String, List<String>> uniqueConstraints(Connection connection, String table) throws SQLException;

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the next
     */
    public NEXT nextval(String sequence);

    /**
     * Lastval.
     *
     * @param args the arguments
     * @return the last
     */
    public LAST lastval(String... args);

    /**
     * Database type.
     *
     * @param connection the connection
     * @return the database type
     */
    public String getDatabaseType(Connection connection);

}
