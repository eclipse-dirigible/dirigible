/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.mongodb.jdbc.util;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.bson.BsonType;
import org.bson.BsonValue;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBConnection;
import org.eclipse.dirigible.mongodb.jdbc.MongoDBResultSetMetaData;

import com.mongodb.MongoClient;

/**
 * The Class SingleColumnStaticResultSet.
 */
public class SingleColumnStaticResultSet implements ResultSet {

    /** The iterable. */
    private Iterator<String> iterable;

    /** The current record index. */
    private int currentRecordIndex;

    /** The current record. */
    private String currentRecord;

    /**
     * Instantiates a new single column static result set.
     *
     * @param iterable the iterable
     */
    public SingleColumnStaticResultSet(Iterator<String> iterable) {
        this.iterable = iterable;
    }

    /**
     * Unwrap.
     *
     * @param <T> the generic type
     * @param iface the iface
     * @return the t
     * @throws SQLException the SQL exception
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
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
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Next.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean next() throws SQLException {
        boolean hasNext = this.iterable.hasNext();
        if (hasNext) {
            this.currentRecord = this.iterable.next();
            this.currentRecordIndex++;
        }
        return hasNext;
    }

    /**
     * Close.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void close() throws SQLException {
        while (this.iterable.hasNext())
            this.iterable.next();
    }

    /**
     * Was null.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean wasNull() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the string.
     *
     * @param columnIndex the column index
     * @return the string
     * @throws SQLException the SQL exception
     */
    @Override
    public String getString(int columnIndex) throws SQLException {
        return this.getString(null);
    }

    /**
     * Gets the boolean.
     *
     * @param columnIndex the column index
     * @return the boolean
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return this.getBoolean(null);
    }

    /**
     * Gets the byte.
     *
     * @param columnIndex the column index
     * @return the byte
     * @throws SQLException the SQL exception
     */
    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return this.getByte(null);
    }

    /**
     * Gets the short.
     *
     * @param columnIndex the column index
     * @return the short
     * @throws SQLException the SQL exception
     */
    @Override
    public short getShort(int columnIndex) throws SQLException {
        return this.getShort(null);
    }

    /**
     * Gets the int.
     *
     * @param columnIndex the column index
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int getInt(int columnIndex) throws SQLException {
        return this.getInt(null);
    }

    /**
     * Gets the long.
     *
     * @param columnIndex the column index
     * @return the long
     * @throws SQLException the SQL exception
     */
    @Override
    public long getLong(int columnIndex) throws SQLException {
        return this.getLong(null);
    }

    /**
     * Gets the float.
     *
     * @param columnIndex the column index
     * @return the float
     * @throws SQLException the SQL exception
     */
    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return this.getFloat(null);
    }

    /**
     * Gets the double.
     *
     * @param columnIndex the column index
     * @return the double
     * @throws SQLException the SQL exception
     */
    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return this.getDouble(null);
    }

    /**
     * Gets the big decimal.
     *
     * @param columnIndex the column index
     * @param scale the scale
     * @return the big decimal
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return this.getBigDecimal(null);
    }

    /**
     * Gets the bytes.
     *
     * @param columnIndex the column index
     * @return the bytes
     * @throws SQLException the SQL exception
     */
    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return this.getBytes(null);
    }

    /**
     * Gets the date.
     *
     * @param columnIndex the column index
     * @return the date
     * @throws SQLException the SQL exception
     */
    @Override
    public Date getDate(int columnIndex) throws SQLException {
        return this.getDate(null);
    }

    /**
     * Gets the time.
     *
     * @param columnIndex the column index
     * @return the time
     * @throws SQLException the SQL exception
     */
    @Override
    public Time getTime(int columnIndex) throws SQLException {
        return this.getTime(null);
    }

    /**
     * Gets the timestamp.
     *
     * @param columnIndex the column index
     * @return the timestamp
     * @throws SQLException the SQL exception
     */
    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return this.getTimestamp(null);
    }

    /**
     * Gets the ascii stream.
     *
     * @param columnIndex the column index
     * @return the ascii stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return this.getAsciiStream(null);
    }

    /**
     * Gets the unicode stream.
     *
     * @param columnIndex the column index
     * @return the unicode stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return this.getUnicodeStream(null);
    }

    /**
     * Gets the binary stream.
     *
     * @param columnIndex the column index
     * @return the binary stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return this.getBinaryStream(null);
    }

    /**
     * Gets the string.
     *
     * @param columnLabel the column label
     * @return the string
     * @throws SQLException the SQL exception
     */
    @Override
    public String getString(String columnLabel) throws SQLException {
        return this.currentRecord;
    }

    /**
     * Gets the boolean.
     *
     * @param columnLabel the column label
     * @return the boolean
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return Boolean.parseBoolean(this.currentRecord);
    }

    /**
     * Gets the byte.
     *
     * @param columnLabel the column label
     * @return the byte
     * @throws SQLException the SQL exception
     */
    @Override
    public byte getByte(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the short.
     *
     * @param columnLabel the column label
     * @return the short
     * @throws SQLException the SQL exception
     */
    @Override
    public short getShort(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the int.
     *
     * @param columnLabel the column label
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int getInt(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the long.
     *
     * @param columnLabel the column label
     * @return the long
     * @throws SQLException the SQL exception
     */
    @Override
    public long getLong(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the float.
     *
     * @param columnLabel the column label
     * @return the float
     * @throws SQLException the SQL exception
     */
    @Override
    public float getFloat(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the double.
     *
     * @param columnLabel the column label
     * @return the double
     * @throws SQLException the SQL exception
     */
    @Override
    public double getDouble(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the big decimal.
     *
     * @param columnLabel the column label
     * @param scale the scale
     * @return the big decimal
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the bytes.
     *
     * @param columnLabel the column label
     * @return the bytes
     * @throws SQLException the SQL exception
     */
    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the date.
     *
     * @param columnLabel the column label
     * @return the date
     * @throws SQLException the SQL exception
     */
    @Override
    public Date getDate(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the time.
     *
     * @param columnLabel the column label
     * @return the time
     * @throws SQLException the SQL exception
     */
    @Override
    public Time getTime(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the timestamp.
     *
     * @param columnLabel the column label
     * @return the timestamp
     * @throws SQLException the SQL exception
     */
    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the ascii stream.
     *
     * @param columnLabel the column label
     * @return the ascii stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the unicode stream.
     *
     * @param columnLabel the column label
     * @return the unicode stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the binary stream.
     *
     * @param columnLabel the column label
     * @return the binary stream
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the warnings.
     *
     * @return the warnings
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Clear warnings.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void clearWarnings() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the cursor name.
     *
     * @return the cursor name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getCursorName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the meta data.
     *
     * @return the meta data
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        MongoDBResultSetMetaData metadata = new MongoDBResultSetMetaData("");
        metadata.setColumnCount(1);
        metadata.keys()
                .put("_", BsonType.STRING);
        return metadata;
    }

    /**
     * Gets the object.
     *
     * @param columnIndex the column index
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getObject(null);
    }

    /**
     * Gets the object.
     *
     * @param columnLabel the column label
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return this.currentRecord;
    }

    /**
     * Find column.
     *
     * @param columnLabel the column label
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int findColumn(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the character stream.
     *
     * @param columnIndex the column index
     * @return the character stream
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the character stream.
     *
     * @param columnLabel the column label
     * @return the character stream
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the big decimal.
     *
     * @param columnIndex the column index
     * @return the big decimal
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the big decimal.
     *
     * @param columnLabel the column label
     * @return the big decimal
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Checks if is before first.
     *
     * @return true, if is before first
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isBeforeFirst() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Checks if is after last.
     *
     * @return true, if is after last
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isAfterLast() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Checks if is first.
     *
     * @return true, if is first
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isFirst() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Checks if is last.
     *
     * @return true, if is last
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isLast() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Before first.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void beforeFirst() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * After last.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void afterLast() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * First.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean first() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Last.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean last() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the row.
     *
     * @return the row
     * @throws SQLException the SQL exception
     */
    @Override
    public int getRow() throws SQLException {
        return this.currentRecordIndex;
    }

    /**
     * Absolute.
     *
     * @param row the row
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean absolute(int row) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Relative.
     *
     * @param rows the rows
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean relative(int rows) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Previous.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean previous() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Sets the fetch direction.
     *
     * @param direction the new fetch direction
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the fetch direction.
     *
     * @return the fetch direction
     * @throws SQLException the SQL exception
     */
    @Override
    public int getFetchDirection() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Sets the fetch size.
     *
     * @param rows the new fetch size
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFetchSize(int rows) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the fetch size.
     *
     * @return the fetch size
     * @throws SQLException the SQL exception
     */
    @Override
    public int getFetchSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the type.
     *
     * @return the type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getType() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the concurrency.
     *
     * @return the concurrency
     * @throws SQLException the SQL exception
     */
    @Override
    public int getConcurrency() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Row updated.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean rowUpdated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Row inserted.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean rowInserted() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Row deleted.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean rowDeleted() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Update null.
     *
     * @param columnIndex the column index
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNull(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update boolean.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update byte.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update short.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update int.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update long.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update float.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update double.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update big decimal.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update string.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update bytes.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update date.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update time.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update timestamp.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update object.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param scaleOrLength the scale or length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update object.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update null.
     *
     * @param columnLabel the column label
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNull(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update boolean.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update byte.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update short.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update int.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update long.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update float.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update double.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update big decimal.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update string.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update bytes.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update date.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update time.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update timestamp.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update object.
     *
     * @param columnLabel the column label
     * @param x the x
     * @param scaleOrLength the scale or length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update object.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Insert row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void insertRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Delete row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void deleteRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Refresh row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void refreshRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Cancel row updates.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void cancelRowUpdates() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Move to insert row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void moveToInsertRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Move to current row.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void moveToCurrentRow() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the statement.
     *
     * @return the statement
     * @throws SQLException the SQL exception
     */
    @Override
    public Statement getStatement() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the object.
     *
     * @param columnIndex the column index
     * @param map the map
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the ref.
     *
     * @param columnIndex the column index
     * @return the ref
     * @throws SQLException the SQL exception
     */
    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the blob.
     *
     * @param columnIndex the column index
     * @return the blob
     * @throws SQLException the SQL exception
     */
    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the clob.
     *
     * @param columnIndex the column index
     * @return the clob
     * @throws SQLException the SQL exception
     */
    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the array.
     *
     * @param columnIndex the column index
     * @return the array
     * @throws SQLException the SQL exception
     */
    @Override
    public Array getArray(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the object.
     *
     * @param columnLabel the column label
     * @param map the map
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the ref.
     *
     * @param columnLabel the column label
     * @return the ref
     * @throws SQLException the SQL exception
     */
    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the blob.
     *
     * @param columnLabel the column label
     * @return the blob
     * @throws SQLException the SQL exception
     */
    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the clob.
     *
     * @param columnLabel the column label
     * @return the clob
     * @throws SQLException the SQL exception
     */
    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the array.
     *
     * @param columnLabel the column label
     * @return the array
     * @throws SQLException the SQL exception
     */
    @Override
    public Array getArray(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the date.
     *
     * @param columnIndex the column index
     * @param cal the cal
     * @return the date
     * @throws SQLException the SQL exception
     */
    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the date.
     *
     * @param columnLabel the column label
     * @param cal the cal
     * @return the date
     * @throws SQLException the SQL exception
     */
    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the time.
     *
     * @param columnIndex the column index
     * @param cal the cal
     * @return the time
     * @throws SQLException the SQL exception
     */
    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the time.
     *
     * @param columnLabel the column label
     * @param cal the cal
     * @return the time
     * @throws SQLException the SQL exception
     */
    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the timestamp.
     *
     * @param columnIndex the column index
     * @param cal the cal
     * @return the timestamp
     * @throws SQLException the SQL exception
     */
    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the timestamp.
     *
     * @param columnLabel the column label
     * @param cal the cal
     * @return the timestamp
     * @throws SQLException the SQL exception
     */
    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the url.
     *
     * @param columnIndex the column index
     * @return the url
     * @throws SQLException the SQL exception
     */
    @Override
    public URL getURL(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the url.
     *
     * @param columnLabel the column label
     * @return the url
     * @throws SQLException the SQL exception
     */
    @Override
    public URL getURL(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update ref.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ref.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update array.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update array.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the row id.
     *
     * @param columnIndex the column index
     * @return the row id
     * @throws SQLException the SQL exception
     */
    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the row id.
     *
     * @param columnLabel the column label
     * @return the row id
     * @throws SQLException the SQL exception
     */
    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update row id.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update row id.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the holdability.
     *
     * @return the holdability
     * @throws SQLException the SQL exception
     */
    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Update N string.
     *
     * @param columnIndex the column index
     * @param nString the n string
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N string.
     *
     * @param columnLabel the column label
     * @param nString the n string
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnIndex the column index
     * @param nClob the n clob
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnLabel the column label
     * @param nClob the n clob
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the n clob.
     *
     * @param columnIndex the column index
     * @return the n clob
     * @throws SQLException the SQL exception
     */
    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the n clob.
     *
     * @param columnLabel the column label
     * @return the n clob
     * @throws SQLException the SQL exception
     */
    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the sqlxml.
     *
     * @param columnIndex the column index
     * @return the sqlxml
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the sqlxml.
     *
     * @param columnLabel the column label
     * @return the sqlxml
     * @throws SQLException the SQL exception
     */
    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update SQLXML.
     *
     * @param columnIndex the column index
     * @param xmlObject the xml object
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update SQLXML.
     *
     * @param columnLabel the column label
     * @param xmlObject the xml object
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the n string.
     *
     * @param columnIndex the column index
     * @return the n string
     * @throws SQLException the SQL exception
     */
    @Override
    public String getNString(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the n string.
     *
     * @param columnLabel the column label
     * @return the n string
     * @throws SQLException the SQL exception
     */
    @Override
    public String getNString(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the n character stream.
     *
     * @param columnIndex the column index
     * @return the n character stream
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the n character stream.
     *
     * @param columnLabel the column label
     * @return the n character stream
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Update N character stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N character stream.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnIndex the column index
     * @param inputStream the input stream
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnLabel the column label
     * @param inputStream the input stream
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnIndex the column index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnIndex the column index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N character stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N character stream.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnIndex the column index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update ascii stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update binary stream.
     *
     * @param columnLabel the column label
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update character stream.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnIndex the column index
     * @param inputStream the input stream
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update blob.
     *
     * @param columnLabel the column label
     * @param inputStream the input stream
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnIndex the column index
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update clob.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnIndex the column index
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Update N clob.
     *
     * @param columnLabel the column label
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the object.
     *
     * @param <T> the generic type
     * @param columnIndex the column index
     * @param type the type
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the object.
     *
     * @param <T> the generic type
     * @param columnLabel the column label
     * @param type the type
     * @return the object
     * @throws SQLException the SQL exception
     */
    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

}
