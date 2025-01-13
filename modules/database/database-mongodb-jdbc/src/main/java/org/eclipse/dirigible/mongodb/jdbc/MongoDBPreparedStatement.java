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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The Class MongoDBPreparedStatement.
 */
public class MongoDBPreparedStatement extends MongoDBStatement implements PreparedStatement {

    /** The p stmnt string. */
    String pStmntString;

    /** The parameters. */
    SortedMap<Integer, Object> parameters = new TreeMap<Integer, Object>();

    /** The rs. */
    private ResultSet rs;

    /**
     * Instantiates a new mongo DB prepared statement.
     *
     * @param conn the conn
     * @param sql the sql
     */
    public MongoDBPreparedStatement(MongoDBConnection conn, String sql) {
        super(conn);
        this.pStmntString = sql;
    }

    /**
     * Execute.
     *
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean execute() throws SQLException {
        return super.execute(this.pStmntString);
    }

    /**
     * Execute query.
     *
     * @return the result set
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSet executeQuery() throws SQLException {
        this.rs = super.executeQuery(this.pStmntString);
        return rs;
    }

    /**
     * Execute update.
     *
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int executeUpdate() throws SQLException {
        return super.executeUpdate(this.pStmntString);
    }

    /**
     * Gets the result set.
     *
     * @return the result set
     */
    @Override
    public ResultSet getResultSet() {
        return this.rs;
    }

    /**
     * Gets the parameter meta data.
     *
     * @return the parameter meta data
     * @throws SQLException the SQL exception
     */
    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        ParameterMetaData paramsMetadata = new MongoDBParameterMetaData(parameters);
        return paramsMetadata;
    }

    /**
     * Clear parameters.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void clearParameters() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the null.
     *
     * @param parameterIndex the parameter index
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the boolean.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the byte.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the short.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the int.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the long.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the float.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the double.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the big decimal.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the string.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the bytes.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the date.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the time.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the timestamp.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the ascii stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the unicode stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the binary stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the object.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param targetSqlType the target sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the object.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Adds the batch.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void addBatch() throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the character stream.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the ref.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the blob.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the clob.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the array.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Gets the meta data.
     *
     * @return the meta data
     * @throws SQLException the SQL exception
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Sets the date.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the time.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the timestamp.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the null.
     *
     * @param parameterIndex the parameter index
     * @param sqlType the sql type
     * @param typeName the type name
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the URL.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the row id.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N string.
     *
     * @param parameterIndex the parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N character stream.
     *
     * @param parameterIndex the parameter index
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N clob.
     *
     * @param parameterIndex the parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the clob.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the blob.
     *
     * @param parameterIndex the parameter index
     * @param inputStream the input stream
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N clob.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the SQLXML.
     *
     * @param parameterIndex the parameter index
     * @param xmlObject the xml object
     * @throws SQLException the SQL exception
     */
    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the object.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param targetSqlType the target sql type
     * @param scaleOrLength the scale or length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the ascii stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the binary stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the character stream.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the ascii stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the binary stream.
     *
     * @param parameterIndex the parameter index
     * @param x the x
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the character stream.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N character stream.
     *
     * @param parameterIndex the parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the clob.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the blob.
     *
     * @param parameterIndex the parameter index
     * @param inputStream the input stream
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        // TODO Auto-generated method stub

    }

    /**
     * Sets the N clob.
     *
     * @param parameterIndex the parameter index
     * @param reader the reader
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        // TODO Auto-generated method stub

    }

}
