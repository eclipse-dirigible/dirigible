/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.helpers;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class for common DataSource operations. An instance represents a single DataSource.
 */
public class DatabaseQueryHelper {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseQueryHelper.class);

    /**
     * The Enum Config.
     */
    public enum Config {

        /** The Show table content script. */
        ShowTableContentScript,
        /** The Schema filter script. */
        SchemaFilterScript;
    }

    /**
     * Instantiates a new database query helper.
     */
    private DatabaseQueryHelper() {

    }

    /**
     * The Interface Filter.
     *
     * @param <T> the generic type
     */
    public interface Filter<T> {

        /**
         * Accepts.
         *
         * @param t the t
         * @return true, if successful
         */
        boolean accepts(T t);
    }

    /**
     * The Interface RequestExecutionCallback.
     */
    public interface RequestExecutionCallback {

        /**
         * Update done.
         *
         * @param recordsCount the records count
         */
        void updateDone(int recordsCount);

        /**
         * Query done.
         *
         * @param rs the rs
         */
        void queryDone(ResultSet rs);

        /**
         * Error.
         *
         * @param t the t
         */
        void error(Throwable t);
    }

    /**
     * Executes a single SQL statement. The callbacks are on queryDone in case of query or updateDone in
     * case of update, and on error. The method does not iterate on the result set and its pointer is in
     * its initial position. It is up to the callback to do something with it.
     *
     * @param connection the connection
     * @param sql the SQL expression
     * @param isQuery whether it is a query or update
     * @param callback the callback
     */
    public static void executeSingleStatement(Connection connection, String sql, boolean isQuery, RequestExecutionCallback callback) {
        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                if (isQuery) {
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        callback.queryDone(resultSet);
                    }
                } else {
                    preparedStatement.executeUpdate();
                    callback.updateDone(preparedStatement.getUpdateCount());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to execute SQL [{}]", sql, e);
            callback.error(e);
        }
    }

    /**
     * Executes a single SQL procedure. The callbacks are on queryDone in case of query or updateDone in
     * case of update, and on error. The method does not iterate on the result set and its pointer is in
     * its initial position. It is up to the callback to do something with it.
     *
     * @param connection the connection
     * @param sql the SQL expression
     * @param callback the callback
     */
    public static void executeSingleProcedure(Connection connection, String sql, RequestExecutionCallback callback) {
        ResultSet resultSet = null;
        try {
            try (CallableStatement callableStatement = connection.prepareCall(sql)) {
                resultSet = callableStatement.executeQuery();
                boolean hasMoreResults = false;
                do {
                    callback.queryDone(resultSet);
                    resultSet.close();
                    hasMoreResults = callableStatement.getMoreResults();
                    if (hasMoreResults) {
                        resultSet = callableStatement.getResultSet();
                    }
                } while (hasMoreResults);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(sql);
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            callback.error(e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Callback interface for the
     * {@link DatabaseQueryHelper#executeQueryStatement(Connection, String, ResultSetIteratorCallback)}
     * method.
     */
    public interface ResultSetIteratorCallback {

        /**
         * On query done.
         *
         * @param conn the conn
         * @param table the table
         */
        void onQueryDone(Connection conn, List<NavigableMap<String, Object>> table);

        /**
         * On row construction.
         *
         * @param conn the conn
         * @param row the row
         */
        void onRowConstruction(Connection conn, NavigableMap<String, Object> row);

        /**
         * On error.
         *
         * @param conn the conn
         * @param t the t
         */
        void onError(Connection conn, Throwable t);
    }

    /**
     * Unlike executeSingleStatement(String, boolean, RequestIteratorCallback), this method iterates on
     * the ResultSet and produces a table data structure in the form of a list of ordered key-value
     * tuples. Schematically it looks like this:
     *
     * <PRE>
     * [[{column 1:value A}, {column 2: value B}, {column 3: value C}],
     *  [{column 1:value D}, {column 2: value E}, {column 3: value F}],
     *  [{column 1:value G}, {column 2: value I}, {column 3: value H}]]
     * </PRE>
     *
     * The callbacks are on completing the whole data structure, on error, and on each row construction.
     *
     * @param connection the connection
     * @param sql the SQL expression
     * @param callback the callback
     */
    public static void executeQueryStatement(Connection connection, String sql, ResultSetIteratorCallback callback) {
        try {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    int columnsCount = resultSet.getMetaData()
                                                .getColumnCount();
                    List<NavigableMap<String, Object>> table = new ArrayList<NavigableMap<String, Object>>();
                    NavigableMap<String, Object> row = new TreeMap<String, Object>();
                    for (int i = 1; i <= columnsCount; i++) {
                        row.put(resultSet.getMetaData()
                                         .getColumnName(i),
                                resultSet.getObject(i));
                        callback.onRowConstruction(connection, row);
                        table.add(row);
                    }
                    callback.onQueryDone(connection, table);
                }
            }
        } catch (Exception e) {
            callback.onError(connection, e);
        }
    }

}
