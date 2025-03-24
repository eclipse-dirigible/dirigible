/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

@Component
class DirigibleCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCleaner.class);

    private final DataSourcesManager dataSourcesManager;

    DirigibleCleaner(DataSourcesManager dataSourcesManager) {
        this.dataSourcesManager = dataSourcesManager;
    }

    public void cleanup() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();

        if (defaultDataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
            deleteSchemas(defaultDataSource);
        }
        deleteDirigibleFolder();
    }

    public static void deleteDirigibleFolder() {
        String dirigibleFolder = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue() + File.separator + "dirigible";
        String skippedDirPath = dirigibleFolder + File.separator + "repository" + File.separator + "index";
        LOGGER.info("Deleting dirigible folder [{}] by skipping [{}]", dirigibleFolder, skippedDirPath);
        try {
            FileUtil.deleteFolder(dirigibleFolder, skippedDirPath);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete dirigible folder [{}] by skipping [{}]", dirigibleFolder, skippedDirPath, ex);
        }
    }

    private void deleteSchemas(DirigibleDataSource dataSource) {
        Set<String> schemas = getSchemas(dataSource);
        schemas.remove("INFORMATION_SCHEMA");
        schemas.remove("information_schema");
        schemas.removeIf(s -> s.startsWith("pg_"));

        LOGGER.debug("Will drop schemas [{}] from data source [{}]", schemas, dataSource);
        schemas.forEach(schema -> deleteSchema(schema, dataSource));

        String schemaName = dataSource.isOfType(DatabaseSystem.POSTGRESQL) ? "public" : "PUBLIC";
        createSchema(dataSource, schemaName);
    }

    private void createSchema(DirigibleDataSource dataSource, String schemaName) {
        LOGGER.debug("Will create schema [{}] in [{}]", schemaName, dataSource);
        try (Connection connection = dataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
            String sql = dialect.create()
                                .schema(schemaName)
                                .generate();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create schema [" + schemaName + "] in dataSource [" + dataSource + "] ", ex);
        }
    }

    private Set<String> getSchemas(DirigibleDataSource dataSource) {
        try {
            if (dataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
                return getSchemas(dataSource, "SELECT nspname FROM pg_catalog.pg_namespace");
            } else {
                return getSchemas(dataSource, "SHOW SCHEMAS");
            }
        } catch (SQLException ex) {
            try {
                return getSchemas(dataSource, "SELECT nspname FROM pg_catalog.pg_namespace");
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to get all schemas from data source: " + dataSource, e);
            }
        }
    }

    private Set<String> getSchemas(DataSource dataSource, String sql) throws SQLException {
        Set<String> schemas = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                schemas.add(resultSet.getString(1));
            }
            return schemas;
        }
    }

    private void deleteSchema(String schema, DirigibleDataSource dataSource) {
        LOGGER.info("Will drop schema [{}] from data source [{}]", schema, dataSource);
        try (Connection connection = dataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
            String sql = dialect.drop()
                                .schema(schema)
                                .cascade(true)
                                .generate();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to drop schema [" + schema + "] from dataSource [" + dataSource + "] ", ex);
        }
    }
}
