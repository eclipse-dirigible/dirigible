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

import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

/**
 * The Class Driver.
 */
public class Driver implements java.sql.Driver {

    /** The Constant logger. */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Driver.class);

    /** The Constant MAJOR_VERSION. */
    public static final int MAJOR_VERSION = 1;

    /** The Constant MINOR_VERSION. */
    public static final int MINOR_VERSION = 0;

    static {
        try {
            java.sql.DriverManager.registerDriver(new Driver());
        } catch (SQLException e) {
            logger.error("Error while registering the JDBC Driver", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    public static String getVersion() {
        return "MongoDB " + MAJOR_VERSION + "." + MINOR_VERSION + " JDBC Driver";
    }

    /**
     * Accepts URL.
     *
     * @param url the url
     * @return true, if successful
     * @throws SQLException the SQL exception
     */
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:mongodb:");
    }

    /**
     * Connect.
     *
     * @param url the url
     * @param info the info
     * @return the connection
     * @throws SQLException the SQL exception
     */
    public Connection connect(String url, Properties info) throws SQLException {
        return new MongoDBConnection(url, info);
    }

    /**
     * Gets the property info.
     *
     * @param url the url
     * @param info the info
     * @return the property info
     * @throws SQLException the SQL exception
     */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Jdbc compliant.
     *
     * @return true, if successful
     */
    public boolean jdbcCompliant() {
        return false;
    }

    /**
     * Gets the major version.
     *
     * @return the major version
     */
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    /**
     * Gets the minor version.
     *
     * @return the minor version
     */
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * Gets the parent logger.
     *
     * @return the parent logger
     * @throws SQLFeatureNotSupportedException the SQL feature not supported exception
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("The Driver uses slf4j for logging");
    }

}
