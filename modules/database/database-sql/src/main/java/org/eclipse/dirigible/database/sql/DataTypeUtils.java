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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.text.MessageFormat.format;

/**
 * The Class DataTypeUtils.
 */
public class DataTypeUtils {

    /** The default length. */
    public static final int VARCHAR_DEFAULT_LENGTH = 512;
    /** The Constant DECIMAL_DEFAULT_LENGTH. */
    public static final int DECIMAL_DEFAULT_LENGTH = 100000;
    /** The Constant JSONB. */
    private static final String JSONB = "JSONB";
    /** The Constant JSON. */
    private static final String JSON = "JSON";
    /** The Constant ARRAY. */
    private static final String ARRAY = "ARRAY";
    /** The Constant BYTEA. */
    private static final String BYTEA = "BYTEA";
    /** The Constant BINARY_LARGE_OBJECT. */
    private static final String BINARY_LARGE_OBJECT = "BINARY LARGE OBJECT";
    /** The Constant BINARY_VARYING. */
    private static final String BINARY_VARYING = "BINARY VARYING";
    /** The Constant BINARY. */
    private static final String BINARY = "BINARY";
    /** The Constant BLOB. */
    private static final String BLOB = "BLOB";
    /** The Constant CHARACTER_LARGE_OBJECT. */
    private static final String CHARACTER_LARGE_OBJECT = "CHARACTER LARGE OBJECT";
    /** The Constant CLOB. */
    private static final String CLOB = "CLOB";
    /** The Constant BOOL. */
    private static final String BOOL = "BOOL";
    /** The Constant BOOLEAN. */
    private static final String BOOLEAN = "BOOLEAN";
    /** The Constant DECIMAL. */
    private static final String DECIMAL = "DECIMAL";
    /** The Constant NUMERIC. */
    private static final String NUMERIC = "NUMERIC";
    /** The Constant NUMBER. */
    private static final String NUMBER = "NUMBER";
    /** The Constant FLOAT8. */
    private static final String FLOAT8 = "FLOAT8";
    /** The Constant FLOAT4. */
    private static final String FLOAT4 = "FLOAT4";
    /** The Constant DOUBLE_PRECISION. */
    private static final String DOUBLE_PRECISION = "DOUBLE PRECISION";
    /** The Constant DOUBLE. */
    private static final String DOUBLE = "DOUBLE";
    /** The Constant FLOAT. */
    private static final String FLOAT = "FLOAT";
    /** The Constant REAL. */
    private static final String REAL = "REAL";
    /** The Constant SERIAL8. */
    private static final String SERIAL8 = "SERIAL8";
    /** The Constant BIGSERIAL. */
    private static final String BIGSERIAL = "BIGSERIAL";
    /** The Constant BIGINT. */
    private static final String BIGINT = "BIGINT";
    /** The Constant INT8. */
    private static final String INT8 = "INT8";
    /** The Constant INT4. */
    private static final String INT4 = "INT4";
    /** The Constant INT2. */
    private static final String INT2 = "INT2";
    /** The Constant INT. */
    private static final String INT = "INT";
    /** The Constant INTEGER. */
    private static final String INTEGER = "INTEGER";
    /** The Constant TINYINT. */
    private static final String TINYINT = "TINYINT";
    /** The Constant SMALLINT. */
    private static final String SMALLINT = "SMALLINT";
    /** The Constant BIT. */
    private static final String BIT = "BIT";
    /** The Constant TIME. */
    private static final String TIME = "TIME";
    /** The Constant TIMESTAMP. */
    private static final String TIMESTAMP = "TIMESTAMP";
    /** The Constant DATE. */
    private static final String DATE = "DATE";
    /** The Constant TEXT. */
    private static final String TEXT = "TEXT";
    /** The Constant CHARACTER. */
    private static final String CHARACTER = "CHARACTER";
    private static final String BPCHAR = "BPCHAR";
    /** The Constant CHAR. */
    private static final String CHAR = "CHAR";
    /** The Constant NVARCHAR. */
    private static final String NVARCHAR = "NVARCHAR";
    /** The Constant CHARACTER_VARYING. */
    private static final String CHARACTER_VARYING = "CHARACTER VARYING";
    /** The Constant VARCHAR. */
    private static final String VARCHAR = "VARCHAR";
    /** The Constant DATABASE_TYPE_TO_DATA_TYPE. */
    private static final Map<Integer, DataType> DATABASE_TYPE_TO_DATA_TYPE = Collections.synchronizedMap(new HashMap<Integer, DataType>());

    /** The Constant STRING_TO_DATABASE_TYPE. */
    private static final Map<String, Integer> STRING_TO_DATABASE_TYPE = Collections.synchronizedMap(new HashMap<String, Integer>());

    /** The Constant JAVA_TYPE_TO_DATABASE_TYPE. */
    private static final Map<Class, Integer> JAVA_TYPE_TO_DATABASE_TYPE = Collections.synchronizedMap(new HashMap<Class, Integer>());

    /** The Constant DATABASE_TYPE_TO_JAVA_TYPE. */
    private static final Map<Integer, Class> DATABASE_TYPE_TO_JAVA_TYPE = Collections.synchronizedMap(new HashMap<Integer, Class>());

    /** The Constant UNIFIED_STRING_FROM_DATABASE_TYPE. */
    private static final Map<String, String> UNIFIED_STRING_FROM_DATABASE_TYPE = Collections.synchronizedMap(new HashMap<String, String>());

    static {
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.VARCHAR, DataType.VARCHAR);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.NVARCHAR, DataType.NVARCHAR);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.CHAR, DataType.CHAR);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.TIMESTAMP, DataType.TIMESTAMP);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.TIMESTAMP, DataType.DATETIME);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.TIME, DataType.TIME);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.INTEGER, DataType.INTEGER);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.INTEGER, DataType.INT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.BIGINT, DataType.BIGINT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.SMALLINT, DataType.SMALLINT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.REAL, DataType.REAL);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.FLOAT, DataType.FLOAT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.DOUBLE, DataType.DOUBLE);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.DATE, DataType.DATE);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.BOOLEAN, DataType.BOOLEAN);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.BLOB, DataType.BLOB);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.DECIMAL, DataType.DECIMAL);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.BIT, DataType.BIT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.ARRAY, DataType.ARRAY);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.TINYINT, DataType.TINYINT);
        DATABASE_TYPE_TO_DATA_TYPE.put(Types.NUMERIC, DataType.NUMERIC);

        // chars
        STRING_TO_DATABASE_TYPE.put(VARCHAR, Types.VARCHAR);
        STRING_TO_DATABASE_TYPE.put(CHARACTER_VARYING, Types.VARCHAR);
        STRING_TO_DATABASE_TYPE.put(NVARCHAR, Types.NVARCHAR);
        STRING_TO_DATABASE_TYPE.put(CHAR, Types.CHAR);
        STRING_TO_DATABASE_TYPE.put(BPCHAR, Types.CHAR);
        STRING_TO_DATABASE_TYPE.put(CHARACTER, Types.CHAR);
        STRING_TO_DATABASE_TYPE.put(TEXT, Types.VARCHAR);
        // dates
        STRING_TO_DATABASE_TYPE.put(DATE, Types.DATE);
        STRING_TO_DATABASE_TYPE.put(TIME, Types.TIME);
        STRING_TO_DATABASE_TYPE.put(TIMESTAMP, Types.TIMESTAMP);

        // https://docs.snowflake.com/en/sql-reference/data-types-datetime#label-datatypes-timestamp-variations
        STRING_TO_DATABASE_TYPE.put("TIMESTAMP_NTZ", Types.TIMESTAMP);
        STRING_TO_DATABASE_TYPE.put("TIMESTAMPNTZ", Types.TIMESTAMP);
        STRING_TO_DATABASE_TYPE.put("TIMESTAMP WITHOUT TIME ZONE", Types.TIMESTAMP);
        STRING_TO_DATABASE_TYPE.put("DATETIME", Types.TIMESTAMP);

        STRING_TO_DATABASE_TYPE.put("DATETIME2", Types.TIMESTAMP); // MSSQL

        // ints
        STRING_TO_DATABASE_TYPE.put(BIT, Types.BIT);
        STRING_TO_DATABASE_TYPE.put(SMALLINT, Types.SMALLINT);
        STRING_TO_DATABASE_TYPE.put(TINYINT, Types.SMALLINT);
        STRING_TO_DATABASE_TYPE.put(INTEGER, Types.INTEGER);
        STRING_TO_DATABASE_TYPE.put(INT, Types.INTEGER);
        STRING_TO_DATABASE_TYPE.put(INT2, Types.INTEGER);
        STRING_TO_DATABASE_TYPE.put(INT4, Types.INTEGER);
        STRING_TO_DATABASE_TYPE.put(INT8, Types.BIGINT);
        STRING_TO_DATABASE_TYPE.put(BIGINT, Types.BIGINT);
        STRING_TO_DATABASE_TYPE.put(BIGSERIAL, Types.BIGINT);
        STRING_TO_DATABASE_TYPE.put(SERIAL8, Types.BIGINT);
        // floats
        STRING_TO_DATABASE_TYPE.put(REAL, Types.REAL);
        STRING_TO_DATABASE_TYPE.put(FLOAT, Types.FLOAT);
        STRING_TO_DATABASE_TYPE.put(DOUBLE, Types.DOUBLE);
        STRING_TO_DATABASE_TYPE.put(DOUBLE_PRECISION, Types.DOUBLE);
        STRING_TO_DATABASE_TYPE.put(FLOAT4, Types.REAL);
        STRING_TO_DATABASE_TYPE.put(FLOAT8, Types.DOUBLE);
        STRING_TO_DATABASE_TYPE.put(NUMERIC, Types.NUMERIC);
        STRING_TO_DATABASE_TYPE.put(DECIMAL, Types.DECIMAL);
        STRING_TO_DATABASE_TYPE.put(NUMBER, Types.DECIMAL);
        // booleans
        STRING_TO_DATABASE_TYPE.put(BOOLEAN, Types.BOOLEAN);
        STRING_TO_DATABASE_TYPE.put(BOOL, Types.BOOLEAN);
        // clobs
        STRING_TO_DATABASE_TYPE.put(CLOB, Types.CLOB);
        STRING_TO_DATABASE_TYPE.put(CHARACTER_LARGE_OBJECT, Types.CLOB);
        // blobs
        STRING_TO_DATABASE_TYPE.put(BLOB, Types.BLOB);
        STRING_TO_DATABASE_TYPE.put(BINARY, Types.BLOB);
        STRING_TO_DATABASE_TYPE.put(BINARY_VARYING, Types.BLOB);
        STRING_TO_DATABASE_TYPE.put(BINARY_LARGE_OBJECT, Types.BLOB);
        STRING_TO_DATABASE_TYPE.put(BYTEA, Types.BLOB);
        // arrays
        STRING_TO_DATABASE_TYPE.put(ARRAY, Types.ARRAY);
        // json
        STRING_TO_DATABASE_TYPE.put(JSON, Types.OTHER);
        STRING_TO_DATABASE_TYPE.put(JSONB, Types.OTHER);

        JAVA_TYPE_TO_DATABASE_TYPE.put(String.class, Types.VARCHAR);
        JAVA_TYPE_TO_DATABASE_TYPE.put(String.class, Types.NVARCHAR);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Integer.class, Types.INTEGER);
        JAVA_TYPE_TO_DATABASE_TYPE.put(int.class, Types.INTEGER);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Long.class, Types.BIGINT);
        JAVA_TYPE_TO_DATABASE_TYPE.put(long.class, Types.BIGINT);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Float.class, Types.FLOAT);
        JAVA_TYPE_TO_DATABASE_TYPE.put(float.class, Types.FLOAT);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Double.class, Types.DOUBLE);
        JAVA_TYPE_TO_DATABASE_TYPE.put(double.class, Types.DOUBLE);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Boolean.class, Types.BOOLEAN);
        JAVA_TYPE_TO_DATABASE_TYPE.put(boolean.class, Types.BOOLEAN);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Date.class, Types.TIMESTAMP);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Time.class, Types.TIME);
        JAVA_TYPE_TO_DATABASE_TYPE.put(Timestamp.class, Types.TIMESTAMP);
        JAVA_TYPE_TO_DATABASE_TYPE.put(byte[].class, Types.BLOB);
        JAVA_TYPE_TO_DATABASE_TYPE.put(List.class, Types.ARRAY);

        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.VARCHAR, String.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.NVARCHAR, String.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.CHAR, String.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.TIMESTAMP, Timestamp.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.TIME, Time.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.SMALLINT, int.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.INTEGER, int.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.BIGINT, long.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.REAL, float.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.FLOAT, float.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.DOUBLE, double.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.DECIMAL, double.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.DATE, Date.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.BOOLEAN, boolean.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.BLOB, byte[].class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.DECIMAL, Double.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.BIT, boolean.class);
        DATABASE_TYPE_TO_JAVA_TYPE.put(Types.ARRAY, List.class);

        // varchars
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(CHARACTER_VARYING, VARCHAR);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(NVARCHAR, VARCHAR);
        // chars
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(CHARACTER, CHAR);
        // ints
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(TINYINT, SMALLINT);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(INT, INTEGER);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(INT2, INTEGER);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(INT4, INTEGER);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(INT8, BIGINT);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BIGSERIAL, BIGINT);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(SERIAL8, BIGINT);
        // floats
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(DOUBLE_PRECISION, DOUBLE);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(FLOAT4, REAL);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(FLOAT8, DOUBLE);
        // booleans
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BOOL, BOOLEAN);
        // clobs
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(CHARACTER_LARGE_OBJECT, CLOB);
        // blobs
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BINARY, BLOB);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BINARY_VARYING, BLOB);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BINARY_LARGE_OBJECT, BLOB);
        UNIFIED_STRING_FROM_DATABASE_TYPE.put(BYTEA, BLOB);

    }

    /**
     * Gets the database type.
     *
     * @param type the type
     * @return the database type
     */
    public static DataType getDatabaseType(Integer type) {
        if (isDatabaseTypeSupported(type)) {
            return DATABASE_TYPE_TO_DATA_TYPE.get(type);
        }
        throw new SqlException(format("Type {0} not supported", type));
    }

    /**
     * Checks if is database type supported.
     *
     * @param type the type
     * @return true, if is database type supported
     */
    public static boolean isDatabaseTypeSupported(Integer type) {
        return DATABASE_TYPE_TO_DATA_TYPE.containsKey(type);
    }

    /**
     * Gets the java type by database type.
     *
     * @param type the type
     * @return the java type by database type
     */
    public static Class getJavaTypeByDatabaseType(Integer type) {
        Class clazz = DATABASE_TYPE_TO_JAVA_TYPE.get(type);
        if (clazz == null) {
            throw new SqlException(format("Type {0} does not have mapping to a java type", type));
        }
        return clazz;
    }

    /**
     * Gets the sql type by data type.
     *
     * @param type the type
     * @return the sql type by data type
     * @throws IllegalArgumentException if the type is not supported
     */
    public static Integer getSqlTypeByDataType(String type) throws IllegalArgumentException {
        type = type.toUpperCase();
        if (STRING_TO_DATABASE_TYPE.containsKey(type)) {
            return STRING_TO_DATABASE_TYPE.get(type);
        }
        throw new IllegalArgumentException("Type [" + type + "] not supported");
    }

    /**
     * Gets the database type name by java type.
     *
     * @param clazz the clazz
     * @return the database type name by java type
     */
    public static String getDatabaseTypeNameByJavaType(Class clazz) {
        Integer type = getDatabaseTypeByJavaType(clazz);
        return getDatabaseTypeName(type);
    }

    /**
     * Gets the database type name.
     *
     * @param type the type
     * @return the database type name
     */
    public static String getDatabaseTypeName(Integer type) throws IllegalArgumentException {
        if (isDatabaseTypeSupported(type)) {
            return DATABASE_TYPE_TO_DATA_TYPE.get(type)
                                             .toString();
        }
        String errorMessage = "Type [" + type + "] not supported. Supported types [" + DATABASE_TYPE_TO_DATA_TYPE + "]";
        throw new IllegalArgumentException(errorMessage);
    }

    /**
     * Gets the database type by java type.
     *
     * @param clazz the clazz
     * @return the database type by java type
     */
    public static Integer getDatabaseTypeByJavaType(Class clazz) {
        Integer type = JAVA_TYPE_TO_DATABASE_TYPE.get(clazz);
        if (type == null) {
            for (Entry<Class, Integer> entry : JAVA_TYPE_TO_DATABASE_TYPE.entrySet()) {
                if (entry.getKey()
                         .isAssignableFrom(clazz)) {
                    return entry.getValue();
                }
            }
            throw new SqlException(format("Class {0} does not have mapping to a data type", clazz));
        }
        return type;
    }

    /**
     * Gets the unified database type.
     *
     * @param type the type
     * @return the unified database type
     */
    public static String getUnifiedDatabaseType(String type) {
        if (UNIFIED_STRING_FROM_DATABASE_TYPE.containsKey(type.toUpperCase())) {
            return UNIFIED_STRING_FROM_DATABASE_TYPE.get(type.toUpperCase());
        }
        return type;
    }

    /**
     * Checks if is blob.
     *
     * @param dataType the data type
     * @return true, if is blob
     */
    public static boolean isBlob(String dataType) {
        return DataType.BLOB.isOfType(dataType);
    }

    /**
     * Checks if is boolean.
     *
     * @param dataType the data type
     * @return true, if is boolean
     */
    public static boolean isBoolean(String dataType) {
        return DataType.BOOLEAN.isOfType(dataType);
    }

    /**
     * Checks if is double.
     *
     * @param dataType the data type
     * @return true, if is double
     */
    public static boolean isDouble(String dataType) {
        return DataType.DOUBLE.isOfType(dataType);
    }

    /**
     * Checks if is real.
     *
     * @param dataType the data type
     * @return true, if is real
     */
    public static boolean isReal(String dataType) {
        return DataType.REAL.isOfType(dataType);
    }

    /**
     * Checks if is float.
     *
     * @param dataType the data type
     * @return true, if is real
     */
    public static boolean isFloat(String dataType) {
        return DataType.FLOAT.isOfType(dataType);
    }

    /**
     * Checks if is bigint.
     *
     * @param dataType the data type
     * @return true, if is bigint
     */
    public static boolean isBigint(String dataType) {
        return DataType.BIGINT.isOfType(dataType);
    }

    /**
     * Checks if is smallint.
     *
     * @param dataType the data type
     * @return true, if is smallint
     */
    public static boolean isSmallint(String dataType) {
        return DataType.SMALLINT.isOfType(dataType);
    }

    /**
     * Checks if is integer.
     *
     * @param dataType the data type
     * @return true, if is integer
     */
    public static boolean isInteger(String dataType) {
        return DataType.INTEGER.isOfType(dataType) || DataType.INT.isOfType(dataType);
    }

    /**
     * Checks if is tinyint.
     *
     * @param dataType the data type
     * @return true, if is tinyint
     */
    public static boolean isTinyint(String dataType) {
        return DataType.TINYINT.isOfType(dataType);
    }

    /**
     * Checks if is timestamp.
     *
     * @param dataType the data type
     * @return true, if is timestamp
     */
    public static boolean isTimestamp(String dataType) {
        return DataType.TIMESTAMP.isOfType(dataType);
    }

    /**
     * Checks if is datetime.
     *
     * @param dataType the data type
     * @return true, if is datetime
     */
    public static boolean isDateTime(String dataType) {
        return DataType.DATETIME.isOfType(dataType);
    }

    /**
     * Checks if is time.
     *
     * @param dataType the data type
     * @return true, if is time
     */
    public static boolean isTime(String dataType) {
        return DataType.TIME.isOfType(dataType);
    }

    /**
     * Checks if is date.
     *
     * @param dataType the data type
     * @return true, if is date
     */
    public static boolean isDate(String dataType) {
        return DataType.DATE.isOfType(dataType);
    }

    /**
     * Checks if is char.
     *
     * @param dataType the data type
     * @return true, if is char
     */
    public static boolean isChar(String dataType) {
        return DataType.CHAR.isOfType(dataType);
    }

    /**
     * Checks if is varchar.
     *
     * @param dataType the data type
     * @return true, if is varchar
     */
    public static boolean isVarchar(String dataType) {
        return DataType.VARCHAR.isOfType(dataType);
    }

    /**
     * Checks if is text.
     *
     * @param dataType the data type
     * @return true, if is text
     */
    public static boolean isText(String dataType) {
        return DataType.TEXT.isOfType(dataType);
    }

    /**
     * Checks if is varchar.
     *
     * @param dataType the data type
     * @return true, if is varchar
     */
    public static boolean isCharacterVarying(String dataType) {
        return DataType.CHARACTER_VARYING.isOfType(dataType);
    }

    /**
     * Checks if is nvarchar.
     *
     * @param dataType the data type
     * @return true, if is varchar
     */
    public static boolean isNvarchar(String dataType) {
        return DataType.NVARCHAR.isOfType(dataType);
    }

    /**
     * Checks if is decimal.
     *
     * @param dataType the data type
     * @return true, if is decimal
     */
    public static boolean isDecimal(String dataType) {
        return DataType.DECIMAL.isOfType(dataType);
    }

    /**
     * Checks if is bit.
     *
     * @param dataType the data type
     * @return true, if is bit
     */
    public static boolean isBit(String dataType) {
        return DataType.BIT.isOfType(dataType);
    }

    /**
     * Checks if is array.
     *
     * @param dataType the data type
     * @return true, if is array
     */
    public static boolean isArray(String dataType) {
        return DataType.ARRAY.isOfType(dataType);
    }

    public static boolean isNumeric(String dataType) {
        return DataType.NUMERIC.isOfType(dataType);
    }
}
