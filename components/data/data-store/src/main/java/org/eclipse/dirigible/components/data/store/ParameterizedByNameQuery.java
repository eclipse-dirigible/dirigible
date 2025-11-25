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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.eclipse.dirigible.components.database.ParameterizedByName;
import org.hibernate.query.Query;

/**
 * The Class ParameterizedQuery.
 */
public class ParameterizedByNameQuery implements ParameterizedByName {

    /** The statement this object is wrapping. */
    private final Query statement;

    /**
     * Creates a ParameterizedStatement.
     *
     * @param statement the database statement
     * @throws SQLException if the statement could not be created
     */
    public ParameterizedByNameQuery(Query statement) throws SQLException {
        this.statement = statement;
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    @Override
    public void setObject(String name, Object value) throws SQLException {
        statement.setParameter(name, value);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    @Override
    public void setString(String name, String value) throws SQLException {
        statement.setParameter(name, value, String.class);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setInt(String name, int value) throws SQLException {
        statement.setParameter(name, value, Integer.class);
    }

    /**
     * Sets the byte.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setByte(String name, byte value) throws SQLException {
        statement.setParameter(name, value, Byte.class);
    }

    /**
     * Sets the short.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setShort(String name, short value) throws SQLException {
        statement.setParameter(name, value, Short.class);
    }

    /**
     * Sets the float.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setFloat(String name, float value) throws SQLException {
        statement.setParameter(name, value, Float.class);
    }

    /**
     * Sets the double.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setDouble(String name, double value) throws SQLException {
        statement.setParameter(name, value, Double.class);
    }

    /**
     * Sets the boolean.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBoolean(String name, boolean value) throws SQLException {
        statement.setParameter(name, value, Boolean.class);
    }

    /**
     * Sets the binary stream.
     *
     * @param name parameter name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(String name, InputStream value, int length) throws SQLException {
        statement.setParameter(name, value, InputStream.class);
    }

    /**
     * Sets the binary stream.
     *
     * @param name parameter name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    @Override
    public void setBinaryStream(String name, InputStream value, long length) throws SQLException {
        statement.setParameter(name, value, InputStream.class);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    @Override
    public void setLong(String name, long value) throws SQLException {
        statement.setParameter(name, value, Long.class);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    @Override
    public void setTimestamp(String name, Timestamp value) throws SQLException {
        statement.setParameter(name, value, Timestamp.class);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date)
     */
    @Override
    public void setDate(String name, java.sql.Date value) throws SQLException {
        statement.setParameter(name, value, java.sql.Date.class);
    }

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @param cal parameter cal
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date, Calendar)
     */
    @Override
    public void setDate(String name, java.sql.Date value, Calendar cal) throws SQLException {
        statement.setParameter(name, value, java.sql.Date.class);
    }

    /**
     * Sets the time.
     *
     * @param name parameter name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(String name, java.sql.Time value) throws SQLException {
        statement.setParameter(name, value, java.sql.Time.class);
    }

    /**
     * Sets the time.
     *
     * @param name parameter name
     * @param value the value
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    @Override
    public void setTime(String name, java.sql.Time value, Calendar cal) throws SQLException {
        statement.setParameter(name, value, java.sql.Time.class);
    }

    /**
     * Sets the null.
     *
     * @param name parameter name
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNull(String name, Integer sqlType) throws SQLException {
        statement.setParameter(name, null);
    }

    /**
     * Sets the array.
     *
     * @param name the name
     * @param value the value
     * @param typeName the type name
     * @throws SQLException the SQL exception
     */
    @Override
    public void setArray(String name, List<?> value, String typeName) throws SQLException {
        statement.setParameter(name, value);
    }

}
