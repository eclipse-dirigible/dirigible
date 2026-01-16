/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects;

import org.eclipse.dirigible.database.sql.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The Default SQL Dialect.
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
public class DefaultSqlDialect<SELECT extends SelectBuilder, INSERT extends InsertBuilder, UPDATE extends UpdateBuilder, DELETE extends DeleteBuilder, CREATE extends CreateBranchingBuilder, ALTER extends AlterBranchingBuilder, DROP extends DropBranchingBuilder, NEXT extends NextValueSequenceBuilder, LAST extends LastValueIdentityBuilder>
        implements ISqlDialect<SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, NEXT, LAST> {

    public static final Set<String> RESERVED_KEYWORDS =
            Set.of("and", "or", "not", "in", "between", "is", "null", "case", "when", "then", "else", "end", "exists", "like");

    /** The Constant FUNCTIONS. */
    public static final Set<String> FUNCTIONS = Collections.synchronizedSet(new HashSet<String>(Arrays.asList("ascii", "char_length",
            "character_length", "concat", "concat_ws", "field", "find_in_set", "format", "insert", "instr", "lcase", "left", "length",
            "locate", "lower", "lpad", "ltrim", "mid", "position", "repeat", "replace", "reverse", "right", "rpad", "rtrim", "space",
            "strcmp", "substr", "substring", "substring_index", "trim", "ucase", "upper", "abs", "acos", "asin", "atan", "atan2", "avg",
            "ceil", "ceiling", "cos", "cot", "count", "degrees", "div", "exp", "floor", "greatest", "least", "ln", "log", "log10", "log2",
            "max", "min", "mod", "pi", "pow", "power", "radians", "rand", "round", "sign", "sin", "sqrt", "sum", "tan", "truncate",
            "adddate", "addtime", "curdate", "current_date", "current_time", "current_timestamp", "curtime", "date", "datediff", "date_add",
            "date_format", "date_sub", "day", "dayname", "dayofmonth", "dayofweek", "dayofyear", "extract", "from_days", "hour", "last_day",
            "localtime", "localtimestamp", "makedate", "maketime", "microsecond", "minute", "month", "monthname", "now", "period_add",
            "period_diff", "quarter", "second", "sec_to_time", "str_to_date", "subdate", "subtime", "sysdate", "time", "time_format",
            "time_to_sec", "timediff", "timestamp", "to_days", "week", "weekday", "weekofyear", "year", "yearweek", "bin", "binary", "case",
            "cast", "coalesce", "connection_id", "conv", "convert", "current_user", "database", "if", "ifnull", "isnull", "last_insert_id",
            "nullif", "session_user", "system_user", "user", "version", "and", "or", "between", "binary", "case", "div", "in", "is", "not",
            "null", "like", "rlike", "xor")));

    private static final Logger logger = LoggerFactory.getLogger(DefaultSqlDialect.class);

    /**
     * Select.
     *
     * @return the select
     */
    @Override
    public SELECT select() {
        return (SELECT) new SelectBuilder(this);
    }

    /**
     * Insert.
     *
     * @return the insert
     */
    @Override
    public INSERT insert() {
        return (INSERT) new InsertBuilder(this);
    }

    /**
     * Update.
     *
     * @return the update
     */
    @Override
    public UPDATE update() {
        return (UPDATE) new UpdateBuilder(this);
    }

    /**
     * Delete.
     *
     * @return the delete
     */
    @Override
    public DELETE delete() {
        return (DELETE) new DeleteBuilder(this);
    }

    /**
     * Expression.
     *
     * @return the expression builder
     */
    @Override
    public ExpressionBuilder expression() {
        return new ExpressionBuilder(this);
    }

    /**
     * Creates the.
     *
     * @return the creates the
     */
    @Override
    public CREATE create() {
        return (CREATE) new CreateBranchingBuilder(this);
    }

    /**
     * Alter.
     *
     * @return the alter
     */
    @Override
    public ALTER alter() {
        return (ALTER) new AlterBranchingBuilder(this);
    }

    /**
     * Drop.
     *
     * @return the drop
     */
    @Override
    public DROP drop() {
        return (DROP) new DropBranchingBuilder(this);
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the next
     */
    @Override
    public NEXT nextval(String sequence) {
        return (NEXT) new NextValueSequenceBuilder(this, sequence);
    }

    /**
     * Gets the data type name.
     *
     * @param dataType the data type
     * @return the data type name
     */
    @Override
    public String getDataTypeName(DataType dataType) {

        if (DataType.ENUM.equals(dataType)) {
            logger.info("Enum data type [{}] will be mapped to [{}]", dataType, DataType.NVARCHAR);
            return DataType.NVARCHAR.toString();
        }

        return dataType.toString();
    }

    /**
     * Gets the primary key argument.
     *
     * @return the primary key argument
     */
    @Override
    public String getPrimaryKeyArgument() {
        return KEYWORD_PRIMARY + SPACE + KEYWORD_KEY;
    }

    /**
     * Gets the identity argument.
     *
     * @return the identity argument
     */
    @Override
    public String getIdentityArgument() {
        return KEYWORD_IDENTITY;
    }

    /**
     * Gets the not null argument.
     *
     * @return the not null argument
     */
    @Override
    public String getNotNullArgument() {
        return KEYWORD_NOT + SPACE + KEYWORD_NULL;
    }

    @Override
    public String getAutoincrementArgument() {
        return "AUTO_INCREMENT";
    }

    /**
     * Gets the unique argument.
     *
     * @return the unique argument
     */
    @Override
    public String getUniqueArgument() {
        return KEYWORD_UNIQUE;
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
        return exists(connection, table, DatabaseArtifactTypes.TABLE);
    }

    /**
     * Exists.
     *
     * @param connection the connection
     * @param table the table
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean exists(Connection connection, String table, int type) throws SQLException {
        return exists(connection, connection.getSchema(), table, type);
    }

    /**
     * Exists.
     *
     * @param connection the connection
     * @param schema the schema
     * @param table the table
     * @param type the type
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean exists(Connection connection, String schema, String table, int type) throws SQLException {
        boolean exists = false;
        String normalizeTableName = normalizeTableName(table);
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet resultSet =
                metadata.getTables(null, schema, normalizeTableName, ISqlKeywords.METADATA_TABLE_TYPES.toArray(new String[] {}));
        exists = resultSet != null && resultSet.next();
        if (!exists) {
            resultSet = metadata.getTables(null, schema, normalizeTableName.toUpperCase(),
                    ISqlKeywords.METADATA_TABLE_TYPES.toArray(new String[] {}));
            exists = resultSet != null && resultSet.next();
        }
        return exists;
    }

    /**
     * Normalize table name.
     *
     * @param table the table
     * @return the string
     */
    public static String normalizeTableName(String table) {
        if (table != null && table.startsWith("\"") && table.endsWith("\"")) {
            table = table.substring(1, table.length() - 1);
        }
        if (table.indexOf("\".\"") > 0) {
            table = table.replace("\".\"", ".");
        }
        return table;
    }

    /**
     * Normalize table name.
     *
     * @param table the table
     * @return the string
     */
    public static String normalizeTableNameOnly(String table) {
        if (table != null && table.startsWith("\"") && table.endsWith("\"")) {
            table = table.substring(1, table.length() - 1);
        }
        if (table.indexOf("\".\"") > 0) {
            table = table.replace("\".\"", ".");
        }
        String[] tokens = table.split("\\.");
        if (tokens.length == 2) {
            return tokens[1];
        }
        return table;
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
        String sql = new SelectBuilder(this).column("*")
                                            .schema("information_schema")
                                            .from("schemata")
                                            .where("schema_name = ?")
                                            .build();
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, schema);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next();
    }

    /**
     * Checks if is schema filter supported.
     *
     * @return true, if is schema filter supported
     */
    @Override
    public boolean isSchemaFilterSupported() {
        return false;
    }

    /**
     * Gets the schema filter script.
     *
     * @return the schema filter script
     */
    @Override
    public String getSchemaFilterScript() {
        return null;
    }

    /**
     * Checks if is catalog for schema.
     *
     * @return true, if is catalog for schema
     */
    @Override
    public boolean isCatalogForSchema() {
        return false;
    }

    /**
     * Function current date.
     *
     * @return the string
     */
    @Override
    public String functionCurrentDate() {
        return ISqlKeywords.FUNCTION_CURRENT_DATE;
    }

    /**
     * Function current time.
     *
     * @return the string
     */
    @Override
    public String functionCurrentTime() {
        return ISqlKeywords.FUNCTION_CURRENT_TIME;
    }

    /**
     * Function current timestamp.
     *
     * @return the string
     */
    @Override
    public String functionCurrentTimestamp() {
        return ISqlKeywords.FUNCTION_CURRENT_TIMESTAMP;
    }

    /**
     * Lastval.
     *
     * @param args the args
     * @return the last
     */
    @Override
    public LAST lastval(String... args) {
        return (LAST) new LastValueIdentityBuilder(this);
    }

    /**
     * Checks if is sequence supported.
     *
     * @return true, if is sequence supported
     */
    @Override
    public boolean isSequenceSupported() {
        return true;
    }

    /**
     * Gets the database name.
     *
     * @param connection the connection
     * @return the database name
     */
    @Override
    public String getDatabaseName(Connection connection) {
        try {
            return connection.getMetaData()
                             .getDatabaseProductName();
        } catch (Exception e) {
            throw new SqlException("Cannot retrieve the database name", e);
        }
    }

    /**
     * Checks if is synonym supported.
     *
     * @return true, if is synonym supported
     */
    @Override
    public boolean isSynonymSupported() {
        return true;
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
     * Gets the fuzzy search index.
     *
     * @return the fuzzy search index
     */
    @Override
    public String getFuzzySearchIndex() {
        return " ";
    }

    /**
     * Gets the escape symbol.
     *
     * @return the escape symbol
     */
    @Override
    public char getEscapeSymbol() {
        return '"';
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
        return count(connection, null, table);
    }

    /**
     * Count.
     *
     * @param connection the connection
     * @param schema the schema
     * @param table the table
     * @return the int
     * @throws SQLException the SQL exception
     */
    public int count(Connection connection, String schema, String table) throws SQLException {
        String sql = countQuery(schema, table);
        PreparedStatement statement = connection.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(1);
        }
        throw new SQLException("Cannot calculate the count of records of table: " + table);
    }

    /**
     * Count query.
     *
     * @param schema the schema
     * @param table the table
     * @return the string
     */
    public String countQuery(String schema, String table) {
        String normalizeTableName = normalizeTableName(table);
        return new SelectBuilder(this).column("COUNT(*)")
                                      .from(quoteTableName(normalizeTableName))
                                      .schema(schema)
                                      .build();
    }

    /**
     * Quote table name.
     *
     * @param table the table
     * @return the string
     */
    public static String quoteTableName(String table) {
        table = normalizeTableName(table);
        String[] tokens = table.split("\\.");
        if (tokens.length == 1) {
            return "\"" + table + "\"";
        }
        if (tokens.length == 2) {
            return "\"" + tokens[0] + "\".\"" + tokens[1] + "\"";
        }
        return table;
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
        return statement.executeQuery();
    }

    /**
     * All query.
     *
     * @param table the table
     * @return the string
     */
    @Override
    public String allQuery(String table) {
        return new SelectBuilder(this).column("*")
                                      .from(quoteTableName(table))
                                      .build();
    }

    /**
     * Count query.
     *
     * @param table the table
     * @return the string
     */
    @Override
    public String countQuery(String table) {
        return countQuery(null, table);
    }

    /**
     * Gets the database type.
     *
     * @param connection the connection
     * @return the database type
     */
    @Override
    public String getDatabaseType(Connection connection) {
        return DatabaseType.RDBMS.getName();
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
        throw new SQLFeatureNotSupportedException();
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
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Process SQL.
     *
     * @param dataSource the data source
     * @param schema the schema
     * @param is the is
     * @throws Exception the exception
     */
    @Override
    public void processSQL(DataSource dataSource, String schema, InputStream is, long fileSize) throws Exception {
        long processedSize = 0;
        long startTime = System.currentTimeMillis();
        logger.info("Import of SQL dump started");

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escapeNextChar = false;
        int ch;

        Connection connection = null;

        int updatesCounter = 0;
        int maxUpdatesPerConnection = 1000;

        try {
            connection = dataSource.getConnection();

            while ((ch = reader.read()) != -1) {
                processedSize++;
                if (escapeNextChar) {
                    escapeNextChar = false;
                } else if (ch == '\\') {
                    escapeNextChar = true;
                } else if (ch == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                } else if (ch == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                }

                if (ch == ';' && !inSingleQuote && !inDoubleQuote && !escapeNextChar) {
                    executeUpdate(connection, builder.toString());
                    updatesCounter++;
                    if (updatesCounter >= maxUpdatesPerConnection) {
                        connection.close();
                        connection = dataSource.getConnection();
                        updatesCounter = 0;
                        logger.info(
                                "SQL dump processed " + String.format("%.2f", ((double) processedSize / (double) fileSize) * 100) + "%");
                    }
                    builder.setLength(0);
                } else {
                    builder.append((char) ch);
                }
            }
            if (builder.length() > 0) {
                executeUpdate(connection, builder.toString());
            }
        } catch (Exception e) {
            logger.warn("SQL dump partially processed " + "[" + processedSize + " / " + fileSize + "] "
                    + String.format("%.1f", ((double) processedSize / (double) fileSize) * 100) + " due to an exception for "
                    + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
            logger.warn("Failed to process the following statement:\n" + builder);
            throw e;
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
        reader.close();

        logger.info("SQL dump successfully processed for " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
    }

    /**
     * Execute update.
     *
     * @param connection the connection
     * @param sql the sql
     * @throws SQLException the SQL exception
     */
    private void executeUpdate(Connection connection, String sql) throws SQLException {
        if (sql.trim()
               .length() > 0) {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.executeUpdate();
        }
    }

}
