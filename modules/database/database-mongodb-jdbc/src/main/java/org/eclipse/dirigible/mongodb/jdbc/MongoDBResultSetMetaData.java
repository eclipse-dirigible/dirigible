/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.mongodb.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bson.BsonType;
import org.bson.codecs.BsonTypeClassMap;

/**
 * The Class MongoDBResultSetMetaData.
 */
public class MongoDBResultSetMetaData implements ResultSetMetaData {

    /** The column count. */
    private int columnCount;

    /** The key map. */
    private SortedMap<String, BsonType> keyMap = new TreeMap<String, BsonType>();

    /** The collection name. */
    private String collectionName;

    /** The bson tojava type map. */
    private BsonTypeClassMap bsonTojavaTypeMap = new BsonTypeClassMap();

    /**
     * Instantiates a new mongo DB result set meta data.
     *
     * @param collectionName the collection name
     */
    public MongoDBResultSetMetaData(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param iface the iface
     * @return the t
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
            return (T) this;
        }
        throw new SQLException("No wrapper for " + iface);
    }

    /**
     * Checks if is wrapper for.
     *
     * @param iface the iface
     * @return true, if is wrapper for
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(getClass());
    }

    /**
     * Keys.
     *
     * @return the map
     */
    public Map<String, BsonType> keys() {
        return this.keyMap;
    }

    /**
     * Gets the column count.
     *
     * @return the column count
     * @throws SQLException the SQL exception
     */
    @Override
    public int getColumnCount() throws SQLException {
        return this.columnCount;
    }

    /**
     * Sets the column count.
     *
     * @param count the new column count
     */
    public void setColumnCount(int count) {
        this.columnCount = count;
    }

    /**
     * Checks if is auto increment.
     *
     * @param column the column
     * @return true, if is auto increment
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is case sensitive.
     *
     * @param column the column
     * @return true, if is case sensitive
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is searchable.
     *
     * @param column the column
     * @return true, if is searchable
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isSearchable(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is currency.
     *
     * @param column the column
     * @return true, if is currency
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is nullable.
     *
     * @param column the column
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int isNullable(int column) throws SQLException {
        return columnNullableUnknown;
    }

    /**
     * Checks if is signed.
     *
     * @param column the column
     * @return true, if is signed
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    /**
     * Gets the column display size.
     *
     * @param column the column
     * @return the column display size
     * @throws SQLException the SQL exception
     */
    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return 25;
    }

    /**
     * Gets the column label.
     *
     * @param column the column
     * @return the column label
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getColumnLabel(int column) throws SQLException {
        Entry<String, BsonType> entry = (Entry<String, BsonType>) this.keyMap.entrySet()
                                                                             .toArray()[column - 1];
        return entry.getKey();
    }

    /**
     * Gets the column name.
     *
     * @param column the column
     * @return the column name
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getColumnName(int column) throws SQLException {
        Entry<String, BsonType> entry = (Entry<String, BsonType>) this.keyMap.entrySet()
                                                                             .toArray()[column - 1];
        return entry.getKey();
    }

    /**
     * Gets the schema name.
     *
     * @param column the column
     * @return the schema name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSchemaName(int column) throws SQLException {
        return null;
    }

    /**
     * Gets the precision.
     *
     * @param column the column
     * @return the precision
     * @throws SQLException the SQL exception
     */
    @Override
    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    /**
     * Gets the scale.
     *
     * @param column the column
     * @return the scale
     * @throws SQLException the SQL exception
     */
    @Override
    public int getScale(int column) throws SQLException {
        return 0;
    }

    /**
     * Gets the table name.
     *
     * @param column the column
     * @return the table name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getTableName(int column) throws SQLException {
        return this.collectionName;
    }

    /**
     * Gets the catalog name.
     *
     * @param column the column
     * @return the catalog name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getCatalogName(int column) throws SQLException {
        return null;
    }

    /**
     * Gets the column type.
     *
     * @param column the column
     * @return the column type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getColumnType(int column) throws SQLException {
        Entry<String, BsonType> entry = (Entry<String, BsonType>) this.keyMap.entrySet()
                                                                             .toArray()[column - 1];
        // return entries[column-1].getValue().getValue();//TODO: this returns the BSON type ordinal. What
        // we need is a mapping to SQL types ordinals.
        return this.getSqlType(entry.getValue());
    }

    /**
     * Gets the column type name.
     *
     * @param column the column
     * @return the column type name
     * @throws SQLException the SQL exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getColumnTypeName(int column) throws SQLException {
        Entry<String, BsonType> entry = (Entry<String, BsonType>) this.keyMap.entrySet()
                                                                             .toArray()[column - 1];
        // return entries[column-1].getValue().toString();// return MongoDB specific datatype name.
        return this.getSqlTypeName(entry.getValue());
    }

    /**
     * Checks if is read only.
     *
     * @param column the column
     * @return true, if is read only
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is writable.
     *
     * @param column the column
     * @return true, if is writable
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isWritable(int column) throws SQLException {
        return false;
    }

    /**
     * Checks if is definitely writable.
     *
     * @param column the column
     * @return true, if is definitely writable
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    /**
     * Gets the column class name.
     *
     * @param column the column
     * @return the column class name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getColumnClassName(int column) throws SQLException {
        Entry<String, BsonType> entry = (Entry<String, BsonType>) this.keyMap.entrySet()
                                                                             .toArray()[column - 1];
        return this.bsonTojavaTypeMap.get(entry.getValue())
                                     .getCanonicalName();
    }

    /**
     * Gets the sql type.
     *
     * @param bsonType the bson type
     * @return the sql type
     */
    int getSqlType(BsonType bsonType) {
        switch (bsonType) {
            case OBJECT_ID: {
                return Types.VARCHAR;
            }
            case ARRAY: {
                return Types.ARRAY;
            }
            case BINARY: {
                return Types.BINARY;
            }
            case BOOLEAN: {
                return Types.BOOLEAN;
            }
            /* case DATE_TIME: { return Types.DATE} */
            case DOCUMENT: {
                return Types.OTHER;
            }
            case DOUBLE: {
                return Types.DOUBLE;
            }
            case INT32: {
                return Types.INTEGER;
            }
            case INT64: {
                return Types.INTEGER;
            }
            case STRING: {
                return Types.VARCHAR;
            }
            case TIMESTAMP: {
                return Types.TIMESTAMP;
            }
            default:
                break;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Gets the sql type name.
     *
     * @param bsonType the bson type
     * @return the sql type name
     */
    String getSqlTypeName(BsonType bsonType) {
        switch (bsonType) {
            case ARRAY: {
                return "ARRAY";
            }
            case BINARY: {
                return "BINARY";
            }
            case BOOLEAN: {
                return "BOOLEAN";
            }
            /* case DATE_TIME: { return Types.DATE} */
            case DOCUMENT: {
                return "OTHER";
            }
            case DOUBLE: {
                return "DOUBLE";
            }
            case INT32: {
                return "INTEGER";
            }
            case INT64: {
                return "INTEGER";
            }
            case STRING: {
                return "VARCHAR";
            }
            case TIMESTAMP: {
                return "TIMESTAMP";
            }
            case OBJECT_ID: {
                return "STRING";
            }
            default:
                break;
        }
        return null;
    }

}
