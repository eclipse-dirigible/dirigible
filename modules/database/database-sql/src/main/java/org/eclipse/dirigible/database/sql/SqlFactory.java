/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.database.sql.dialects.DefaultSqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;

/**
 * A factory for creating SQL objects.
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
public class SqlFactory<SELECT extends SelectBuilder, INSERT extends InsertBuilder, UPDATE extends UpdateBuilder, DELETE extends DeleteBuilder, CREATE extends CreateBranchingBuilder, ALTER extends AlterBranchingBuilder, DROP extends DropBranchingBuilder, NEXT extends NextValueSequenceBuilder, LAST extends LastValueIdentityBuilder>
        implements ISqlFactory<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST> {

    /** The dialect. */
    private ISqlDialect<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST> dialect;

    /**
     * Gets the default.
     *
     * @return the default
     */
    public static SqlFactory getDefault() {
        return new SqlFactory();
    }

    /**
     * Gets the native.
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
     * @param dialect the dialect
     * @return the native
     */
    public static <SELECT extends SelectBuilder, INSERT extends InsertBuilder, UPDATE extends UpdateBuilder, DELETE extends DeleteBuilder, CREATE extends CreateBranchingBuilder, ALTER extends AlterBranchingBuilder, DROP extends DropBranchingBuilder, NEXT extends NextValueSequenceBuilder, LAST extends LastValueIdentityBuilder> SqlFactory<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST> getNative(
            ISqlDialect<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST> dialect) {
        return new SqlFactory<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST>(dialect);
    }

    /**
     * Gets the native.
     *
     * @param connection the connection
     * @return the native
     */
    public static SqlFactory getNative(Connection connection) {
        return new SqlFactory(deriveDialect(connection));
    }

    /**
     * Instantiates a new sql factory.
     */
    private SqlFactory() {
        this(new DefaultSqlDialect());
    }

    /**
     * Instantiates a new sql factory.
     *
     * @param dialect the dialect
     */
    private SqlFactory(ISqlDialect dialect) {
        this.dialect = dialect;
    }

    /**
     * Derive dialect.
     *
     * @param connection the connection
     * @return the i sql dialect
     */
    public static ISqlDialect deriveDialect(Connection connection) {
        try {
            return SqlDialectFactory.getDialect(connection);
        } catch (SQLException e) {
            throw new SqlException("Error on deriving the database dialect from the connection", e);
        }

    }

    /**
     * Select.
     *
     * @return the select
     */
    @Override
    public SELECT select() {
        return this.dialect.select();
    }

    /**
     * Insert.
     *
     * @return the insert
     */
    @Override
    public INSERT insert() {
        return this.dialect.insert();
    }

    /**
     * Update.
     *
     * @return the update
     */
    @Override
    public UPDATE update() {
        return this.dialect.update();
    }

    /**
     * Delete.
     *
     * @return the delete
     */
    @Override
    public DELETE delete() {
        return this.dialect.delete();
    }

    /**
     * Expression.
     *
     * @return the expression builder
     */
    @Override
    public ExpressionBuilder expression() {
        return this.dialect.expression();
    }

    /**
     * Creates the.
     *
     * @return the creates the
     */
    @Override
    public CREATE create() {
        return this.dialect.create();
    }

    /**
     * Alter.
     *
     * @return the alter
     */
    @Override
    public ALTER alter() {
        return this.dialect.alter();
    }

    /**
     * Drop.
     *
     * @return the drop
     */
    @Override
    public DROP drop() {
        return this.dialect.drop();
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the next
     */
    @Override
    public NEXT nextval(String sequence) {
        return this.dialect.nextval(sequence);
    }

    /**
     * Exists.
     *
     * @param connection the connection
     * @param table the table
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean existsTable(Connection connection, String table) throws SQLException {
        return this.dialect.existsTable(connection, table);
    }

    /**
     * Exists.
     *
     * @param connection the connection
     * @param name the name
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean exists(Connection connection, String name, int type) throws SQLException {
        return this.dialect.exists(connection, name, type);
    }

    /**
     * Exists.
     *
     * @param connection the connection
     * @param schema the schema
     * @param name the name
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean exists(Connection connection, String schema, String name, int type) throws SQLException {
        return this.dialect.exists(connection, schema, name, type);
    }

    /**
     * Exists schema.
     *
     * @param connection the connection
     * @param schema the schema
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean existsSchema(Connection connection, String schema) throws SQLException {
        return this.dialect.existsSchema(connection, schema);
    }

    /**
     * Lastval.
     *
     * @param args the args
     * @return the last
     */
    @Override
    public LAST lastval(String... args) {
        return this.dialect.lastval(args);
    }

    /**
     * Gets the database type.
     *
     * @param connection the connection
     * @return the database type
     */
    @Override
    public String getDatabaseType(Connection connection) {
        return this.dialect.getDatabaseType(connection);
    }

}
