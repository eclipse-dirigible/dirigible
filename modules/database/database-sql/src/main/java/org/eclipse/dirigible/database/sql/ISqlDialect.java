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

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.NextValueSequenceBuilder;

/**
 * The SQL Dialect interface.
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
public interface ISqlDialect<SELECT extends SelectBuilder, INSERT extends InsertBuilder, UPDATE extends UpdateBuilder, DELETE extends DeleteBuilder, CREATE extends CreateBranchingBuilder, ALTER extends AlterBranchingBuilder, DROP extends DropBranchingBuilder, NEXT extends NextValueSequenceBuilder, LAST extends LastValueIdentityBuilder>
        extends ISqlFactory<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST>, ISqlKeywords {

    /** The Constant FUNCTIONS. */
    Set FUNCTIONS = Collections.synchronizedSet(new HashSet<String>(Arrays.asList("ASCII", "CHAR_LENGTH", "CHARACTER_LENGTH", "CONCAT",
            "CONCAT_WS", "FIELD", "FIND_IN_SET", "FORMAT", "INSERT", "INSTR", "LCASE", "LEFT", "LENGTH", "LOCATE", "LOWER", "LPAD", "LTRIM",
            "MID", "POSITION", "REPEAT", "REPLACE", "REVERSE", "RIGHT", "RPAD", "RTRIM", "SPACE", "STRCMP", "SUBSTR", "SUBSTRING",
            "SUBSTRING_INDEX", "TRIM", "UCASE", "UPPER", "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "AVG", "CEIL", "CEILING", "COS", "COT",
            "COUNT", "DEGREES", "DIV", "EXP", "FLOOR", "GREATEST", "LEAST", "LN", "LOG", "LOG10", "LOG2", "MAX", "MIN", "MOD", "PI", "POW",
            "POWER", "RADIANS", "RAND", "ROUND", "SIGN", "SIN", "SQRT", "SUM", "TAN", "TRUNCATE", "ADDDATE", "ADDTIME", "CURDATE",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURTIME", "DATE", "DATEDIFF", "DATE_ADD", "DATE_FORMAT", "DATE_SUB",
            "DAY", "DAYNAME", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR", "EXTRACT", "FROM_DAYS", "HOUR", "LAST_DAY", "LOCALTIME",
            "LOCALTIMESTAMP", "MAKEDATE", "MAKETIME", "MICROSECOND", "MINUTE", "MONTH", "MONTHNAME", "NOW", "PERIOD_ADD", "PERIOD_DIFF",
            "QUARTER", "SECOND", "SEC_TO_TIME", "STR_TO_DATE", "SUBDATE", "SUBTIME", "SYSDATE", "TIME", "TIME_FORMAT", "TIME_TO_SEC",
            "TIMEDIFF", "TIMESTAMP", "TO_DAYS", "WEEK", "WEEKDAY", "WEEKOFYEAR", "YEAR", "YEARWEEK", "BIN", "BINARY", "CASE", "CAST",
            "COALESCE", "CONNECTION_ID", "CONV", "CONVERT", "CURRENT_USER", "DATABASE", "IF", "IFNULL", "ISNULL", "LAST_INSERT_ID",
            "NULLIF", "SESSION_USER", "SYSTEM_USER", "USER", "VERSION")));

    /**
     * Default implementation returns the direct toString() conversion. It may get overridden for
     * specific database dialects
     *
     * @param dataType the data type
     * @return the data type name
     */
    String getDataTypeName(DataType dataType);

    /**
     * PRIMARY KEY argument for a column for the create table script Default is "PRIMARY KEY".
     *
     * @return the primary key argument
     */
    String getPrimaryKeyArgument();

    /**
     * Identity argument for a column for the create table script Default is "IDENTITY".
     *
     * @return the primary key argument
     */
    String getIdentityArgument();

    /**
     * NOT NULL argument for a column for the create table script Default is "NOT NULL".
     *
     * @return the not null argument
     */
    String getNotNullArgument();

    String getAutoincrementArgument();

    /**
     * UNIQUE argument for a column for the create table script Default is "UNIQUE".
     *
     * @return the unique argument
     */
    String getUniqueArgument();

    /**
     * Check existence of a table.
     *
     * @param connection the current connection
     * @param table the table name
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */
    @Override
    boolean existsTable(Connection connection, String table) throws SQLException;

    /**
     * Check existence of an artifacts.
     *
     * @param connection the current connection
     * @param name the artifact name
     * @param type the artifact type
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */

    @Override
    boolean exists(Connection connection, String name, int type) throws SQLException;

    /**
     * Check existence of a schema.
     *
     * @param connection the current connection
     * @param schema the schema name
     * @return true if the table exists and false otherwise
     * @throws SQLException the SQL exception
     */
    @Override
    boolean existsSchema(Connection connection, String schema) throws SQLException;

    /**
     * Checks if the database is capable of schema-level filtering statements (e.g. to reduce the
     * provisioned schemas down to those that the current user is entitled to see).
     *
     * @return true if the feature is supported, false otherwise
     */
    boolean isSchemaFilterSupported();

    /**
     * If the database supports schema filtering SQL statements (see
     * {@link #isSchemaFilterSupported()}), this method provides the corresponding SQL statement.
     *
     * @return a filtering SQL statement
     */
    String getSchemaFilterScript();

    /**
     * Does this database support catalogs synonymous to schemas.
     *
     * @return whether it is a catalog for schema
     */
    boolean isCatalogForSchema();

    /**
     * Gives the dialect specific name of the CURRENT_DATE function.
     *
     * @return the name of the function
     */
    String functionCurrentDate();

    /**
     * Gives the dialect specific name of the CURRENT_TIME function.
     *
     * @return the name of the function
     */
    String functionCurrentTime();

    /**
     * Gives the dialect specific name of the CURRENT_TIMESTAMP function.
     *
     * @return the name of the function
     */
    String functionCurrentTimestamp();

    /**
     * Checks if the database is capable to create and use Sequences.
     *
     * @return true if the feature is supported, false otherwise
     */
    boolean isSequenceSupported();

    /**
     * Returns the database name.
     *
     * @param connection the active database connection
     * @return the database name
     */
    String getDatabaseName(Connection connection);

    /**
     * Checks if the database is capable to create and use Synonyms.
     *
     * @return true if the feature is supported, false otherwise
     */
    boolean isSynonymSupported();

    /**
     * Returns the function names.
     *
     * @return the list of functions names
     */
    Set<String> getFunctionsNames();

    /**
     * Returns the creation of fuzzy search index.
     *
     * @return the string for creating the Fuzzy Search Index
     */
    String getFuzzySearchIndex();

    /**
     * Returns the escape symbol.
     *
     * @return the string for escape symbol
     */
    char getEscapeSymbol();

    /**
     * Count.
     *
     * @param connection the connection
     * @param table the table
     * @return the count
     * @throws SQLException the SQL exception
     */
    int count(Connection connection, String table) throws SQLException;

    /**
     * Count.
     *
     * @param connection the connection
     * @param table the table
     * @return the result set
     * @throws SQLException the SQL exception
     */
    ResultSet all(Connection connection, String table) throws SQLException;

    /**
     * Count.
     *
     * @param table the table
     * @return the count query
     */
    String countQuery(String table);

    /**
     * All.
     *
     * @param table the table
     * @return the all query
     */
    String allQuery(String table);

    /**
     * Export data.
     *
     * @param connection the connection
     * @param table the table
     * @param output the output
     * @throws Exception Signals that an I/O or SQL exception has occurred.
     */
    void exportData(Connection connection, String table, OutputStream output) throws Exception;

    /**
     * Import data.
     *
     * @param connection the connection
     * @param table the table
     * @param input the input
     * @throws Exception Signals that an I/O or SQL exception has occurred.
     */
    void importData(Connection connection, String table, InputStream input) throws Exception;

    /**
     * Process SQL.
     *
     * @param dataSource the data source
     * @param schema the schema
     * @param is the input stream
     * @param fileSize the file size
     */
    void processSQL(DataSource dataSource, String schema, InputStream is, long fileSize) throws Exception;

}
