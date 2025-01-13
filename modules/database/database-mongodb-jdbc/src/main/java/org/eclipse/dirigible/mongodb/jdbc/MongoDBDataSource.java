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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;

/**
 * The Class MongoDBDataSource.
 */
public class MongoDBDataSource implements DataSource {

    /** The Constant LOG. */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MongoDBDataSource.class);

    static {
        try {
            Class.forName(Driver.class.getCanonicalName());
        } catch (ClassNotFoundException e) {
            LOG.error("MongoDB DataSource unable to load MongoDB JDBC Driver", e);
            throw new RuntimeException(e);
        }
    }

    /** The url. */
    private String url;

    /** The user name. */
    private String userName;

    /** The password. */
    private String password;

    /** The logger. */
    private PrintWriter logger;

    /** The login timeout. */
    private int loginTimeout;

    /**
     * Instantiates a new mongo DB data source.
     */
    public MongoDBDataSource() {}

    /**
     * Instantiates a new mongo DB data source.
     *
     * @param url the url
     * @param userName the user name
     * @param password the password
     */
    public MongoDBDataSource(String url, String userName, String password) {
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the new url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the user name.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name.
     *
     * @param userName the new user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets the log writer.
     *
     * @return the log writer
     * @throws SQLException the SQL exception
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.logger;
    }

    /**
     * Sets the log writer.
     *
     * @param out the new log writer
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logger = out;
    }

    /**
     * Sets the login timeout.
     *
     * @param seconds the new login timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    /**
     * Gets the login timeout.
     *
     * @return the login timeout
     * @throws SQLException the SQL exception
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return this.loginTimeout;
    }

    /**
     * Gets the parent logger.
     *
     * @return the parent logger
     * @throws SQLFeatureNotSupportedException the SQL feature not supported exception
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
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
     * Gets the connection.
     *
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection() throws SQLException {
        return this.getConnection(this.userName, this.password);
    }

    /**
     * Gets the connection.
     *
     * @param username the username
     * @param password the password
     * @return the connection
     * @throws SQLException the SQL exception
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(this.url, this.userName, this.password);
    }

}
