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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

/**
 * The Interface ParameterizedByName.
 */
public interface ParameterizedByName {

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setObject(int, java.lang.Object)
     */
    void setObject(String name, Object value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setString(int, java.lang.String)
     */
    void setString(String name, String value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    void setInt(String name, int value) throws SQLException;

    /**
     * Sets the byte.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setByte(String name, byte value) throws SQLException;

    /**
     * Sets the short.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setShort(String name, short value) throws SQLException;

    /**
     * Sets the float.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setFloat(String name, float value) throws SQLException;

    /**
     * Sets the double.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setDouble(String name, double value) throws SQLException;

    /**
     * Sets the boolean.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setBoolean(String name, boolean value) throws SQLException;

    /**
     * Sets the binary stream.
     *
     * @param name the name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    void setBinaryStream(String name, InputStream value, int length) throws SQLException;

    /**
     * Sets the binary stream.
     *
     * @param name the name
     * @param value the value
     * @param length the length
     * @throws SQLException the SQL exception
     */
    void setBinaryStream(String name, InputStream value, long length) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setInt(int, int)
     */
    void setLong(String name, long value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    void setTimestamp(String name, Timestamp value) throws SQLException;

    /**
     * Sets a parameter.
     *
     * @param name parameter name
     * @param value parameter value
     * @throws SQLException if an error occurred
     * @throws IllegalArgumentException if the parameter does not exist
     * @see PreparedStatement#setDate(int, java.sql.Date)
     */
    void setDate(String name, java.sql.Date value) throws SQLException;

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
    void setDate(String name, java.sql.Date value, Calendar cal) throws SQLException;

    /**
     * Sets the time.
     *
     * @param name the name
     * @param value the value
     * @throws SQLException the SQL exception
     */
    void setTime(String name, java.sql.Time value) throws SQLException;

    /**
     * Sets the time.
     *
     * @param name the name
     * @param value the value
     * @param cal the cal
     * @throws SQLException the SQL exception
     */
    void setTime(String name, java.sql.Time value, Calendar cal) throws SQLException;

    /**
     * Sets the null.
     *
     * @param name the name
     * @param sqlType the sql type
     * @throws SQLException the SQL exception
     */
    void setNull(String name, Integer sqlType) throws SQLException;

    /**
     * Sets the array.
     *
     * @param name the name
     * @param value the value
     * @param typeName the type name
     * @throws SQLException the SQL exception
     */
    void setArray(String name, List<?> value, String typeName) throws SQLException;

}
