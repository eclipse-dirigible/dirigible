/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.h2;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.dirigible.commons.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * H2 Database adapter.
 */
public class H2Database {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(H2Database.class);

    /** The Constant TYPE. */
    public static final String NAME = "h2";

    /** The Constant TYPE. */
    public static final String TYPE = "local";

    /** The Constant DIRIGIBLE_DATABASE_H2_ROOT_FOLDER. */
    public static final String DIRIGIBLE_DATABASE_H2_ROOT_FOLDER = "DIRIGIBLE_DATABASE_H2_ROOT_FOLDER"; //$NON-NLS-1$

    /** The Constant DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT. */
    public static final String DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT = DIRIGIBLE_DATABASE_H2_ROOT_FOLDER + "_DEFAULT"; //$NON-NLS-1$

    /** The Constant DATASOURCES. */
    private static final Map<String, DataSource> DATASOURCES = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor with default root folder - user.dir
     *
     * @throws SQLException in case the database cannot be created
     */
    public H2Database() throws SQLException {
        this(null);
    }

    /**
     * Constructor with root folder parameter.
     *
     * @param rootFolder the root folder
     * @throws SQLException in case the database cannot be created
     */
    public H2Database(String rootFolder) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing the embedded H2 datasource...");
        }

        initialize();

        if (logger.isDebugEnabled()) {
            logger.debug("Embedded H2 datasource initialized.");
        }
    }

    /**
     * Initialize.
     */
    public void initialize() {
        Configuration.loadModuleConfig("/dirigible-database-h2.properties");
        if (logger.isDebugEnabled()) {
            logger.debug(this.getClass()
                             .getCanonicalName()
                    + " module initialized.");
        }
    }

    /**
     * Gets the data source.
     *
     * @param name the name
     * @return the data source
     * @throws SQLException
     */
    public DataSource getDataSource(String name) throws SQLException {
        DataSource dataSource = DATASOURCES.get(name);
        if (dataSource != null) {
            return dataSource;
        }
        dataSource = createDataSource(name);
        return dataSource;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return NAME;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return TYPE;
    }

    /**
     * Creates the data source.
     *
     * @param name the name
     * @return the data source
     * @throws SQLException
     */
    protected DataSource createDataSource(String name) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating an embedded H2 datasource...");
        }
        synchronized (H2Database.class) {
            try {
                String h2Root = prepareRootFolder(name);

                String databaseUrl = Configuration.get("DIRIGIBLE_DATABASE_H2_URL");
                String databaseUsername = Configuration.get("DIRIGIBLE_DATABASE_H2_USERNAME");
                String databasePassword = Configuration.get("DIRIGIBLE_DATABASE_H2_PASSWORD");

                int timeout = Configuration.getAsInt("DIRIGIBLE_DATABASE_DEFAULT_WAIT_TIMEOUT", 180000);

                if ((databaseUrl != null) && (databaseUsername != null) && (databasePassword != null)) {
                    HikariConfig config = new HikariConfig();

                    config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
                    config.setPoolName("H2DBHikariPool");
                    config.setConnectionTestQuery("VALUES 1");
                    config.addDataSourceProperty("URL", databaseUrl + "/" + name + ";LOCK_TIMEOUT=10000");
                    config.addDataSourceProperty("user", databaseUsername);
                    config.addDataSourceProperty("password", databasePassword);
                    config.setMinimumIdle(5);
                    config.setMaximumPoolSize(50);
                    config.setConnectionTimeout(timeout);
                    config.setLeakDetectionThreshold(timeout);
                    config.setIdleTimeout(600000);
                    config.setMaxLifetime(1800000);
                    config.setAutoCommit(true);

                    HikariDataSource ds = new HikariDataSource(config);

                    if (logger.isWarnEnabled()) {
                        logger.warn("Embedded H2 at: {}", h2Root);
                    }

                    DATASOURCES.put(name, ds);
                    return ds;
                } else {
                    throw new SQLException("Invalid datasource parameters provided for H2 database");
                }
            } catch (IOException e) {
                throw new SQLException(e);
            }
        }
    }

    /**
     * Prepare root folder.
     *
     * @param name the name
     * @return the string
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private String prepareRootFolder(String name) throws IOException {
        // TODO validate name parameter
        // TODO get by name from Configuration

        String rootFolder =
                ("DefaultDB".equals(name)) ? DIRIGIBLE_DATABASE_H2_ROOT_FOLDER_DEFAULT : DIRIGIBLE_DATABASE_H2_ROOT_FOLDER + name;
        String h2Root = Configuration.get(rootFolder, name);
        File rootFile = new File(h2Root);
        File parentFile = rootFile.getCanonicalFile()
                                  .getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new IOException(format("Creation of the root folder [{0}] of the embedded H2 database failed.", h2Root));
            }
        }
        return h2Root;
    }

    /**
     * Gets the data sources.
     *
     * @return the data sources
     */
    public Map<String, DataSource> getDataSources() {
        Map<String, DataSource> datasources = new HashMap<>();
        datasources.putAll(DATASOURCES);
        return datasources;
    }

}
