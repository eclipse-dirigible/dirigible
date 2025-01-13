/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mongodb;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.database.sql.DatabaseType;
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.NextValueSequenceBuilder;
import org.eclipse.dirigible.database.sql.dialects.DefaultSqlDialect;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBConnection;
import org.eclipse.dirigible.mongodb.jdbc.util.ExportImportUtil;

/**
 * The MongoDB SQL Dialect.
 */
public class MongoDBSqlDialect extends
        DefaultSqlDialect<SelectBuilder, InsertBuilder, UpdateBuilder, DeleteBuilder, CreateBranchingBuilder, AlterBranchingBuilder, DropBranchingBuilder, NextValueSequenceBuilder, LastValueIdentityBuilder> {

    /** The Constant FUNCTIONS. */
    public static final Set<String> FUNCTIONS = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(new String[] {})));

    /**
     * Checks if is synonym supported.
     *
     * @return true, if is synonym supported
     */
    @Override
    public boolean isSynonymSupported() {
        return false;
    }

    /**
     * Gets the functions names.
     *
     * @return the functions names
     */
    @Override
    public Set<String> getFunctionsNames() {
        return FUNCTIONS;
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
        return false;
    }

    /**
     * Count.
     *
     * @param connection the connection
     * @param table the table
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int count(Connection connection, String table) throws SQLException {
        String sql = countQuery(table);
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(null);
        }
        return -1;
    }

    /**
     * All.
     *
     * @param connection the connection
     * @param table the table
     * @return the result set
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet all(Connection connection, String table) throws SQLException {
        String sql = allQuery(table);
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        return resultSet;
    }

    /**
     * Count query.
     *
     * @param table the table
     * @return the string
     */
    @Override
    public String countQuery(String table) {
        String sql = "{ 'count': '" + normalizeTableNameOnly(table) + "'}";
        return sql;
    }

    /**
     * All query.
     *
     * @param table the table
     * @return the string
     */
    @Override
    public String allQuery(String table) {
        String sql = "{ 'find': '" + normalizeTableNameOnly(table) + "'}";
        return sql;
    }

    /**
     * Gets the database type.
     *
     * @param connection the connection
     * @return the database type
     */
    @Override
    public String getDatabaseType(Connection connection) {
        return DatabaseType.NOSQL.getName();
    }

    /**
     * Export data.
     *
     * @param connection the connection
     * @param table the table
     * @param output the output
     * @throws Exception the exception
     */
    @Override
    public void exportData(Connection connection, String table, OutputStream output) throws Exception {
        ExportImportUtil.exportCollection(connection.unwrap(MongoDBConnection.class), table, output);
    }

    /**
     * Import data.
     *
     * @param connection the connection
     * @param table the table
     * @param input the input
     * @throws Exception the exception
     */
    @Override
    public void importData(Connection connection, String table, InputStream input) throws Exception {
        ExportImportUtil.importCollection(connection.unwrap(MongoDBConnection.class), table, input);
    }

}
