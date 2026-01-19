/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;
import org.eclipse.dirigible.database.sql.dialects.DefaultSqlDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The MSSQL SQL Dialect.
 */
public class MSSQLSqlDialect extends
        DefaultSqlDialect<MSSQLSelectBuilder, InsertBuilder, UpdateBuilder, DeleteBuilder, MSSQLCreateBranchingBuilder, AlterBranchingBuilder, DropBranchingBuilder, MSSQLNextValueSequenceBuilder, LastValueIdentityBuilder> {

    public static final String FUNCTION_CURRENT_DATE = "CAST(GETDATE() AS DATE)"; //$NON-NLS-1$
    public static final String FUNCTION_CURRENT_TIME = "CAST(GETDATE() AS TIME)"; //$NON-NLS-1$
    public static final String FUNCTION_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP"; //$NON-NLS-1$

    public static final Set<String> FUNCTIONS; // functions + keywords

    static {
        FUNCTIONS = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(
                // --- MATH & STATS ---
                "abs", "acos", "asin", "atan", "atan2", "ceiling", "cos", "cot", "degrees", "exp", "floor", "log", "log10", "pi", "power",
                "radians", "rand", "round", "sign", "sin", "sqrt", "square", "tan",

                // --- STRING MANIPULATION (MSSQL Specifics) ---
                "ascii", "char", "charindex", "concat", "concat_ws", "difference", "format", "left", "len", "lower", "ltrim", "nchar",
                "patindex", "quotename", "replace", "replicate", "reverse", "right", "rtrim", "soundex", "space", "str", "stuff",
                "substring", "translate", "trim", "upper", "unicode",

                // --- DATE & TIME ---
                "dateadd", "datediff", "datepart", "datename", "day", "month", "year", "getdate", "getutcdate", "sysdatetime",
                "sysdatetimeoffset", "isdate", "eomonth", "switchoffset", "todatetimeoffset",

                // --- DATA TYPE & CONVERSION ---
                "cast", "convert", "parse", "try_cast", "try_convert", "try_parse", "isnumeric", "isjson",

                // --- NULL HANDLING & LOGIC ---
                "coalesce", "choose", "iif", "isnull", "nullif",

                // --- AGGREGATES ---
                "avg", "count", "count_big", "max", "min", "sum", "stdev", "stdevp", "var", "varp", "string_agg", "checksum_agg",

                // --- SYSTEM & METADATA ---
                "newid", "newsequentialid", "rowcount", "compress", "decompress", "host_id", "host_name", "suser_sname", "user_name",
                "db_name", "object_id")));

        FUNCTIONS.addAll(RESERVED_KEYWORDS);
    }

    /**
     * Creates the.
     *
     * @return the mssql create branching builder
     */
    @Override
    public MSSQLCreateBranchingBuilder create() {
        return new MSSQLCreateBranchingBuilder(this);
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the mssql next value sequence builder
     */
    @Override
    public MSSQLNextValueSequenceBuilder nextval(String sequence) {
        return new MSSQLNextValueSequenceBuilder(this, sequence);
    }

    /**
     * Function current date.
     *
     * @return the string
     */
    @Override
    public String functionCurrentDate() {
        return FUNCTION_CURRENT_DATE;
    }

    @Override
    public String getAutoincrementArgument() {
        return "IDENTITY(1,1)";
    }

    /**
     * Function current time.
     *
     * @return the string
     */
    @Override
    public String functionCurrentTime() {
        return FUNCTION_CURRENT_TIME;
    }

    /**
     * Function current timestamp.
     *
     * @return the string
     */
    @Override
    public String functionCurrentTimestamp() {
        return FUNCTION_CURRENT_TIMESTAMP;
    }

    @Override
    public String getDataTypeName(DataType dataType) {
        return switch (dataType) {
            // --- String & Large Text ---
            case CHARACTER_LARGE_OBJECT, CLOB, TEXT -> "NVARCHAR(MAX)";
            case NVARCHAR -> "NVARCHAR";
            case VARCHAR -> "VARCHAR";

            // --- Binary & Blobs ---
            case BLOB, BINARY_LARGE_OBJECT, BYTEA, VARBINARY, BINARY_VARYING -> "VARBINARY(MAX)";

            // --- Boolean ---
            case BOOLEAN, BOOL -> "BIT";

            // --- Numbers ---
            case DOUBLE, DOUBLE_PRECISION -> "FLOAT";
            case REAL -> "REAL"; // 4-byte float in MSSQL

            // --- Integers ---
            case INT8, BIGINT -> "BIGINT";
            case INT, INT4, INTEGER -> "INT";
            case INT2, SMALLINT -> "SMALLINT";
            case TINYINT, BYTE -> "TINYINT"; // Note: MSSQL TINYINT is 0-255

            // --- Date & Time ---
            case TIMESTAMP, DATETIME -> "DATETIME2"; // DATETIME2 is preferred over DATETIME

            default -> super.getDataTypeName(dataType);
        };
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

    @Override
    public boolean existsSchema(Connection connection, String schema) throws SQLException {
        // sys.schemas is the primary metadata view in MSSQL
        String sql = "SELECT * FROM sys.schemas WHERE name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public boolean existsTable(Connection connection, String table) throws SQLException {
        String sql = """
                SELECT 1 FROM sys.tables t
                JOIN sys.schemas s ON t.schema_id = s.schema_id
                WHERE t.name = ? AND s.name = ?""";

        // MSSQL defaults to 'dbo' if no schema is specified in the connection
        String schema = connection.getSchema();
        if (schema == null) {
            schema = "dbo";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public MSSQLSelectBuilder select() {
        return new MSSQLSelectBuilder(this);
    }
}
