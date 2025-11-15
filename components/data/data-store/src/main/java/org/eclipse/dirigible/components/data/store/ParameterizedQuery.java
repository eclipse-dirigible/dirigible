/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import org.eclipse.dirigible.components.database.Parameterized;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.hibernate.query.BindableType;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.QueryParameter;

public class ParameterizedQuery implements Parameterized {

    /** The statement this object is wrapping. */
    private final Query statement;

    /**
     * Creates a ParameterizedStatement.
     *
     * @param statement the database statement
     * @throws SQLException if the statement could not be created
     */
    public ParameterizedQuery(Query statement) throws SQLException {
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
        statement.setParameter(index, value);
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
        statement.setParameter(index, value, String.class);
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
        statement.setParameter(index, value, Integer.class);
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
        statement.setParameter(index, value, Byte.class);
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
        statement.setParameter(index, value, Short.class);
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
        statement.setParameter(index, value, Float.class);
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
        statement.setParameter(index, value, Double.class);
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
        statement.setParameter(index, value, Boolean.class);
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
        statement.setParameter(index, value, InputStream.class);
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
        statement.setParameter(index, value, InputStream.class);
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
        statement.setParameter(index, value, Long.class);
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
        statement.setParameter(index, value, Timestamp.class);
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
        statement.setParameter(index, value, java.sql.Date.class);
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
        statement.setParameter(index, value, java.sql.Date.class);
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
        statement.setParameter(index, value, java.sql.Time.class);
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
        statement.setParameter(index, value, java.sql.Time.class);
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
        statement.setParameter(index, null);
    }

    @Override
    public void setBigDecimal(int index, BigDecimal value) throws SQLException {
        statement.setParameter(index, value, BigDecimal.class);
    }

    @Override
    public void setObject(int index, Object value, int targetSqlType) throws SQLException {
        statement.setParameter(index, value);
    }

    @Override
    public Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException("This functionality is not available via Hibernate persistence layer");
    }

    @Override
    public void addBatch() throws SQLException {
        throw new UnsupportedOperationException("This functionality is not available via Hibernate persistence layer");
    }

    @Override
    public int getParameterCount() throws SQLException {
        ParameterMetadata parameterMetaData = statement.getParameterMetadata();
        int sqlParametersCount = parameterMetaData.getParameterCount();
        return sqlParametersCount;
    }

    @Override
    public int getParameterType(int sqlParamIndex) throws SQLException {
        ParameterMetadata parameterMetaData = statement.getParameterMetadata();
        QueryParameter<?> queryParameter = parameterMetaData.getQueryParameter(sqlParamIndex);
        BindableType bindableSqlType = parameterMetaData.getInferredParameterType(queryParameter);
        Class javaType = bindableSqlType.getBindableJavaType();
        int sqlType = DataTypeUtils.getDatabaseTypeByJavaType(javaType);
        return sqlType;
    }

}
