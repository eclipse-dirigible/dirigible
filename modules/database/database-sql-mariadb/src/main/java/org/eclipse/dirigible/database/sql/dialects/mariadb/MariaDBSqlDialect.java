/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mariadb;

import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.eclipse.dirigible.database.sql.dialects.DefaultSqlDialect;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * The MariaDB SQL Dialect.
 */
public class MariaDBSqlDialect extends
        DefaultSqlDialect<MariaDBSelectBuilder, InsertBuilder, MariaDBUpdateBuilder, MariaDBDeleteBuilder, MariaDBCreateBranchingBuilder, AlterBranchingBuilder, MariaDBDropBranchingBuilder, MariaDBNextValueSequenceBuilder, MariaDBLastValueIdentityBuilder> {

    /** The Constant MARIADB_KEYWORD_IDENTITY. */
    private static final String MARIA_DB_KEYWORD_IDENTITY = "AUTO_INCREMENT";

    /** The Constant FUNCTIONS. */
    public static final Set<String> FUNCTIONS = Collections.synchronizedSet(new HashSet<>(Arrays.asList("DATABASE", "USER", "SYSTEM_USER",
            "SESSION_USER", "LAST_INSERT_ID", "VERSION", "DIV", "ABS", "ACOS", "ASIN", "ATAN", "ATAN2", "CEIL", "CEILING", "CONV", "COS",
            "COT", "CRC32", "DEGREES", "EXP", "FLOOR", "GREATEST", "LEAST", "LN", "LOG", "LOG10", "LOG2", "MOD", "OCT", "PI", "POW",
            "POWER", "RADIANS", "RAND", "ROUND", "SIGN", "SIN", "SQRT", "TAN", "TRUNCATE", "ASCII", "BIN", "BIT_LENGTH", "CAST",
            "CHARACTER_LENGTH", "CHAR_LENGTH", "CONCAT", "CONCAT_WS", "CONVERT", "ELT", "EXPORT_SET", "EXTRACTVALUE", "FIELD",
            "FIND_IN_SET", "FORMAT", "FROM_BASE64", "HEX", "INSTR", "LCASE", "LEFT", "LENGTH", "LIKE", "LOAD_FILE", "LOCATE", "LOWER",
            "LPAD", "LTRIM", "MAKE_SET", "MATCH AGAINST", "MID", "NOT LIKE", "NOT REGEXP", "OCTET_LENGTH", "ORD", "POSITION", "QUOTE",
            "REPEAT", "REPLACE", "REVERSE", "RIGHT", "RPAD", "RTRIM", "SOUNDEX", "SOUNDS LIKE", "SPACE", "STRCMP", "SUBSTR", "SUBSTRING",
            "SUBSTRING_INDEX", "TO_BASE64", "TRIM", "UCASE", "UNHEX", "UPDATEXML", "UPPER", "WEIGHT_STRING", "ADDDATE", "ADDTIME",
            "CONVERT_TZ", "CURDATE", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURTIME", "DATEDIFF", "DATE_ADD", "DATE_FORMAT",
            "DATE_SUB", "DAY", "DAYNAME", "DAYOFMONTH", "DAYOFWEEK", "DAYOFYEAR", "EXTRACT", "FROM_DAYS", "FROM_UNIXTIME", "GET_FORMAT",
            "HOUR", "LAST_DAY", "LOCALTIME", "LOCALTIMESTAMP", "MAKEDATE", "MAKETIME", "MICROSECOND", "MINUTE", "MONTH", "MONTHNAME", "NOW",
            "PERIOD_ADD", "PERIOD_DIFF", "QUARTER", "SECOND", "SEC_TO_TIME", "STR_TO_DATE", "SUBDATE", "SUBTIME", "SYSDATE", "TIMEDIFF",
            "TIMESTAMPADD", "TIMESTAMPDIFF", "TIME_FORMAT", "TIME_TO_SEC", "TO_DAYS", "TO_SECONDS", "UNIX_TIMESTAMP", "UTC_DATE",
            "UTC_TIME", "UTC_TIMESTAMP", "WEEK", "WEEKDAY", "WEEKOFYEAR", "YEAR", "YEARWEEK", "COUNT", "AND", "OR", "BETWEEN", "IS", "NOT",
            "NULL", "AVG", "MAX", "MIN")));

    /**
     * Creates the.
     *
     * @return the my SQL create branching builder
     */
    @Override
    public MariaDBCreateBranchingBuilder create() {
        return new MariaDBCreateBranchingBuilder(this);
    }

    /**
     * Drop.
     *
     * @return the my SQL drop branching builder
     */
    @Override
    public MariaDBDropBranchingBuilder drop() {
        return new MariaDBDropBranchingBuilder(this);
    }

    /**
     * Nextval.
     *
     * @param sequence the sequence
     * @return the my SQL next value sequence builder
     */
    @Override
    public MariaDBNextValueSequenceBuilder nextval(String sequence) {
        return new MariaDBNextValueSequenceBuilder(this, sequence);
    }

    /**
     * Lastval.
     *
     * @param args the args
     * @return the my SQL last value identity builder
     */
    @Override
    public MariaDBLastValueIdentityBuilder lastval(String... args) {
        return new MariaDBLastValueIdentityBuilder(this);
    }

    /**
     * Gets the identity argument.
     *
     * @return the identity argument
     */
    @Override
    public String getIdentityArgument() {
        return MARIA_DB_KEYWORD_IDENTITY;
    }

    /**
     * Checks if is sequence supported.
     *
     * @return true, if is sequence supported
     */
    @Override
    public boolean isSequenceSupported() {
        return false;
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
        table = normalizeTableName(table);
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet resultSet = metadata.getTables(null, null, DefaultSqlDialect.normalizeTableName(table.toUpperCase()),
                ISqlKeywords.METADATA_TABLE_TYPES.toArray(new String[] {}));
        return resultSet.next();
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
     * Gets the escape symbol.
     *
     * @return the escape symbol
     */
    @Override
    public char getEscapeSymbol() {
        return '`';
    }

    @Override
    public boolean isCatalogForSchema() {
        return true;
    }

    @Override
    public MariaDBSelectBuilder select() {
        return new MariaDBSelectBuilder(this);
    }

    @Override
    public MariaDBUpdateBuilder update() {
        return new MariaDBUpdateBuilder(this);
    }

    @Override
    public MariaDBDeleteBuilder delete() {
        return new MariaDBDeleteBuilder(this);
    }

    @Override
    public MariaDBInsertBuilder insert() {
        return new MariaDBInsertBuilder(this);
    }

}
