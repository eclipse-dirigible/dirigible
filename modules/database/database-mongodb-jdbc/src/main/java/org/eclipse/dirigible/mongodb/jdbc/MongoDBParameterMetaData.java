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

import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.util.SortedMap;

/**
 * The Class MongoDBParameterMetaData.
 */
public class MongoDBParameterMetaData implements ParameterMetaData {

    /**
     * Instantiates a new mongo DB parameter meta data.
     *
     * @param parameters the parameters
     */
    public MongoDBParameterMetaData(SortedMap<Integer, Object> parameters) {
        // TODO Auto-generated constructor stub
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
     * Gets the parameter count.
     *
     * @return the parameter count
     * @throws SQLException the SQL exception
     */
    @Override
    public int getParameterCount() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Checks if is nullable.
     *
     * @param param the param
     * @return the int
     * @throws SQLException the SQL exception
     */
    @Override
    public int isNullable(int param) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Checks if is signed.
     *
     * @param param the param
     * @return true, if is signed
     * @throws SQLException the SQL exception
     */
    @Override
    public boolean isSigned(int param) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Gets the precision.
     *
     * @param param the param
     * @return the precision
     * @throws SQLException the SQL exception
     */
    @Override
    public int getPrecision(int param) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the scale.
     *
     * @param param the param
     * @return the scale
     * @throws SQLException the SQL exception
     */
    @Override
    public int getScale(int param) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the parameter type.
     *
     * @param param the param
     * @return the parameter type
     * @throws SQLException the SQL exception
     */
    @Override
    public int getParameterType(int param) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Gets the parameter type name.
     *
     * @param param the param
     * @return the parameter type name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getParameterTypeName(int param) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the parameter class name.
     *
     * @param param the param
     * @return the parameter class name
     * @throws SQLException the SQL exception
     */
    @Override
    public String getParameterClassName(int param) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Gets the parameter mode.
     *
     * @param param the param
     * @return the parameter mode
     * @throws SQLException the SQL exception
     */
    @Override
    public int getParameterMode(int param) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

}
