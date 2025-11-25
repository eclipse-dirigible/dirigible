/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * The Class ParameterizedStatement.
 */
public class ParameterizedStatement implements ParameterizedByIndex {

    /** The statement this object is wrapping. */
    private final PreparedStatement statement;

    /**
     * Creates a ParameterizedStatement.
     *
     * @param statement the database statement
     * @throws SQLException if the statement could not be created
     */
    public ParameterizedStatement(PreparedStatement statement) throws SQLException {
        this.statement = statement;
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    @Override
    public void setObject(int index, Object value) throws SQLException {
        statement.setObject(index, value);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    @Override
    public void setString(int index, String value) throws SQLException {
        statement.setString(index, value);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setInt(int index, int value) throws SQLException {
        statement.setInt(index, value);
    }

    /**
     * Sets the byte.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setByte(int index, byte value) throws SQLException {
        statement.setByte(index, value);
    }

    /**
     * Sets the short.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setShort(int index, short value) throws SQLException {
        statement.setShort(index, value);
    }

    /**
     * Sets the float.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFloat(int index, float value) throws SQLException {
        statement.setFloat(index, value);
    }

    /**
     * Sets the double.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDouble(int index, double value) throws SQLException {
        statement.setDouble(index, value);
    }

    /**
     * Sets the boolean.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBoolean(int index, boolean value) throws SQLException {
        statement.setBoolean(index, value);
    }

    /**
     * Sets the binary stream.
     *
     * @param index parameter index
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(int index, InputStream value, int length) throws SQLException {
        statement.setBinaryStream(index, value, length);
    }

    /**
     * Sets the binary stream.
     *
     * @param index parameter index
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(int index, InputStream value, long length) throws SQLException {
        statement.setBinaryStream(index, value, length);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setLong(int index, long value) throws SQLException {
        statement.setLong(index, value);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    @Override
    public void setTimestamp(int index, Timestamp value) throws SQLException {
        statement.setTimestamp(index, value);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date)
     */
    @Override
    public void setDate(int index, java.sql.Date value) throws SQLException {
        statement.setDate(index, value);
    }

    /**
     * Sets a parameter.
     *
     * @param index parameter index
     * @param value parameter value
     * @param cal parameter cal
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date, Calendar)
     */
    @Override
    public void setDate(int index, java.sql.Date value, Calendar cal) throws SQLException {
        statement.setDate(index, value, cal);
    }

    /**
     * Sets the time.
     *
     * @param index parameter index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(int index, java.sql.Time value) throws SQLException {
        statement.setTime(index, value);
    }

    /**
     * Sets the time.
     *
     * @param index parameter index
     * @param value the value
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(int index, java.sql.Time value, Calendar cal) throws SQLException {
        statement.setTime(index, value, cal);
    }

    /**
     * Sets the null.
     *
     * @param index parameter index
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNull(int index, Integer sqlType) throws SQLException {
        statement.setNull(index, sqlType);
    }

    /**
     * Sets the big decimal.
     *
     * @param index the index
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBigDecimal(int index, BigDecimal value) throws SQLException {
        statement.setBigDecimal(index, value);
    }

    /**
     * Sets the object.
     *
     * @param index the index
     * @param value the value
     * @param targetSqlType the target sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setObject(int index, Object value, int targetSqlType) throws SQLException {
        statement.setObject(index, value, targetSqlType);
    }

    /**
     * Sets the array.
     *
     * @param index the index
     * @param value the value
     * @param typeName the type name
     * @throws SQLException the SQL exception
     */
    @Override
    public void setArray(int index, List<?> value, String typeName) throws SQLException {
        Connection connection = getConnection();
        Array array = connection.createArrayOf(typeName, value.toArray());
        statement.setArray(index, array);
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    /**
     * Adds the batch.
     *
     * @throws SQLException the SQL exception
     */
    @Override
    public void addBatch() throws SQLException {
        statement.addBatch();
    }

    /**
     * Gets the parameter count.
     *
     * @return the parameter count
     * @throws SQLException the SQL exception
     */
    @Override
    public int getParameterCount() throws SQLException {
        ParameterMetaData parameterMetaData = statement.getParameterMetaData();
        int sqlParametersCount = parameterMetaData.getParameterCount();
        return sqlParametersCount;
    }

    /**
     * Gets the parameter type.
     *
     * @param sqlParamIndex the sql param index
     * @return the parameter type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getParameterType(int sqlParamIndex) throws SQLException {
        ParameterMetaData parameterMetaData = statement.getParameterMetaData();
        int sqlType = parameterMetaData.getParameterType(sqlParamIndex);
        return sqlType;
    }

}
