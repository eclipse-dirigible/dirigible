/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.postgres;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.eclipse.dirigible.database.sql.builders.records.UpdateBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.LastValueIdentityBuilder;
import org.eclipse.dirigible.database.sql.dialects.DefaultSqlDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PostgreSQL SQL Dialect.
 */
public class PostgresSqlDialect extends
        DefaultSqlDialect<SelectBuilder, InsertBuilder, UpdateBuilder, DeleteBuilder, PostgresCreateBranchingBuilder, AlterBranchingBuilder, DropBranchingBuilder, PostgresNextValueSequenceBuilder, LastValueIdentityBuilder> {

    /** The Constant FUNCTION_CURRENT_DATE. */
    public static final String FUNCTION_CURRENT_DATE = "current_date"; //$NON-NLS-1$
    /** The Constant FUNCTION_CURRENT_TIME. */
    public static final String FUNCTION_CURRENT_TIME = "current_time"; //$NON-NLS-1$
    /** The Constant FUNCTION_CURRENT_TIMESTAMP. */
    public static final String FUNCTION_CURRENT_TIMESTAMP = "current_timestamp"; //$NON-NLS-1$
    /** The Constant FUNCTIONS. */
    public static final Set<String> FUNCTIONS = Collections.synchronizedSet(new HashSet<String>(Arrays.asList("abs", "cbrt", "ceil",
            "ceiling", "degrees", "div", "exp", "floor", "ln", "log", "mod", "pi", "power", "radians", "round", "sign", "sqrt", "trunc",
            "width_bucket", "count", "sum", "avg", "min", "max", "and", "or", "between", "binary", "case", "div", "in", "is", "not", "null",
            "like", "rlike", "xor", "random", "setseed", "acos", "asin", "atan", "atan2", "cos", "cot", "sin", "tan", "bit_length",
            "char_length", "lower", "octet_length", "overlay", "position", "substring", "trim", "upper", "ascii", "btrim", "chr", "concat",
            "concat_ws", "convert", "convert_from", "convert_to", "decode", "encode", "format", "initcap", "left", "length", "lpad",
            "ltrim", "md5", "pg_client_encoding", "quote_ident", "quote_literal", "quote_nullable", "regexp_replace",
            "regexp_split_to_array", "repeat", "replace", "reverse", "right", "rpad", "rtrim", "split_part", "substr", "to_ascii", "to_hex",
            "translate", "ascii_to_mic", "ascii_to_utf8", "big5_to_euc_tw", "big5_to_mic", "big5_to_utf8", "euc_cn_to_mic",
            "euc_cn_to_utf8", "euc_jp_to_mic", "euc_jp_to_sjis", "euc_jp_to_utf8", "euc_kr_to_mic", "euc_kr_to_utf8", "euc_tw_to_big5",
            "euc_tw_to_mic", "euc_tw_to_utf8", "gb18030_to_utf8", "gbk_to_utf8", "iso_8859_10_to_utf8", "iso_8859_13_to_utf8",
            "iso_8859_14_to_utf8", "iso_8859_15_to_utf8", "iso_8859_16_to_utf8", "iso_8859_1_to_mic", "iso_8859_1_to_utf8",
            "iso_8859_2_to_mic", "iso_8859_2_to_utf8", "iso_8859_2_to_windows_1250", "iso_8859_3_to_mic", "iso_8859_3_to_utf8",
            "iso_8859_4_to_mic", "iso_8859_4_to_utf8", "iso_8859_5_to_koi8_r", "iso_8859_5_to_mic", "iso_8859_5_to_utf8",
            "iso_8859_5_to_windows_1251", "iso_8859_5_to_windows_866", "iso_8859_6_to_utf8", "iso_8859_7_to_utf8", "iso_8859_8_to_utf8",
            "iso_8859_9_to_utf8", "johab_to_utf8", "koi8_r_to_iso_8859_5", "koi8_r_to_mic", "koi8_r_to_utf8", "koi8_r_to_windows_1251",
            "koi8_r_to_windows_866", "koi8_u_to_utf8", "mic_to_ascii", "mic_to_big5", "mic_to_euc_cn", "mic_to_euc_jp", "mic_to_euc_kr",
            "mic_to_euc_tw", "mic_to_iso_8859_1", "mic_to_iso_8859_2", "mic_to_iso_8859_3", "mic_to_iso_8859_4", "mic_to_iso_8859_5",
            "mic_to_koi8_r", "mic_to_sjis", "mic_to_windows_1250", "mic_to_windows_1251", "mic_to_windows_866", "sjis_to_euc_jp",
            "sjis_to_mic", "sjis_to_utf8", "tcvn_to_utf8", "uhc_to_utf8", "utf8_to_ascii", "utf8_to_big5", "utf8_to_euc_cn",
            "utf8_to_euc_jp", "utf8_to_euc_kr", "utf8_to_euc_tw", "utf8_to_gb18030", "utf8_to_gbk", "utf8_to_iso_8859_1",
            "utf8_to_iso_8859_10", "utf8_to_iso_8859_13", "utf8_to_iso_8859_14", "utf8_to_iso_8859_15", "utf8_to_iso_8859_16",
            "utf8_to_iso_8859_2", "utf8_to_iso_8859_3", "utf8_to_iso_8859_4", "utf8_to_iso_8859_5", "utf8_to_iso_8859_6",
            "utf8_to_iso_8859_7", "utf8_to_iso_8859_8", "utf8_to_iso_8859_9", "utf8_to_johab", "utf8_to_koi8_r", "utf8_to_koi8_u",
            "utf8_to_sjis", "utf8_to_tcvn", "utf8_to_uhc", "utf8_to_windows_1250", "utf8_to_windows_1251", "utf8_to_windows_1252",
            "utf8_to_windows_1253", "utf8_to_windows_1254", "utf8_to_windows_1255", "utf8_to_windows_1256", "utf8_to_windows_1257",
            "utf8_to_windows_866", "utf8_to_windows_874", "windows_1250_to_iso_8859_2", "windows_1250_to_mic", "windows_1250_to_utf8",
            "windows_1251_to_iso_8859_5", "windows_1251_to_koi8_r", "windows_1251_to_mic", "windows_1251_to_utf8",
            "windows_1251_to_windows_866", "windows_1252_to_utf8", "windows_1256_to_utf8", "windows_866_to_iso_8859_5",
            "windows_866_to_koi8_r", "windows_866_to_mic", "windows_866_to_utf8", "windows_866_to_windows_1251", "windows_874_to_utf8",
            "euc_jis_2004_to_utf8", "utf8_to_euc_jis_2004", "shift_jis_2004_to_utf8", "utf8_to_shift_jis_2004",
            "euc_jis_2004_to_shift_jis_2004", "shift_jis_2004_to_euc_jis_2004", "get_bit", "get_byte", "set_bit")));

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresSqlDialect.class);

    /**
     * Creates the.
     *
     * @return the postgres create branching builder
     */
    @Override
    public PostgresCreateBranchingBuilder create() {
        return new PostgresCreateBranchingBuilder(this);
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the postgres next value sequence builder
     */
    @Override
    public PostgresNextValueSequenceBuilder nextval(String sequence) {
        return new PostgresNextValueSequenceBuilder(this, sequence);
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
        return "GENERATED BY DEFAULT AS IDENTITY";
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

    /**
     * Gets the data type name.
     *
     * @param dataType the data type
     * @return the data type name
     */
    @Override
    public String getDataTypeName(DataType dataType) {
        return switch (dataType) {
            case BLOB -> "bytea";
            case DOUBLE -> "DOUBLE PRECISION";
            case CHARACTER_LARGE_OBJECT -> "TEXT";
            case NVARCHAR -> "VARCHAR";
            default -> super.getDataTypeName(dataType);
        };
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
        boolean exists = false;
        ResultSet resultSet = null;

        String normalizeTableName = normalizeTableName(table);
        DatabaseMetaData metadata = connection.getMetaData();

        String schema = connection.getSchema();
        resultSet = metadata.getTables(null, schema, normalizeTableName, ISqlKeywords.METADATA_TABLE_TYPES.toArray(new String[] {}));
        exists = resultSet != null && resultSet.next();
        if (!exists) {
            resultSet = metadata.getTables(null, schema, DefaultSqlDialect.normalizeTableName(normalizeTableName.toLowerCase()),
                    ISqlKeywords.METADATA_TABLE_TYPES.toArray(new String[] {}));
            exists = resultSet != null && resultSet.next();
        }
        return exists;
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

}
