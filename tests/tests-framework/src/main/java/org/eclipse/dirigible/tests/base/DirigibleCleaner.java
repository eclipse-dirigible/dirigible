/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import jakarta.persistence.EntityManagerFactory;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DatabaseSystem;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.tests.framework.util.FileUtil;
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
    private final EntityManagerFactory entityManagerFactory;

    DirigibleCleaner(DataSourcesManager dataSourcesManager, EntityManagerFactory entityManagerFactory) {
        this.dataSourcesManager = dataSourcesManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    public void cleanup() {
        try {
            clearEntityManagerCaches();

            // clean up SystemDB
            DirigibleDataSource systemDataSource = dataSourcesManager.getSystemDataSource();
            if (systemDataSource.isOfType(DatabaseSystem.H2)) {
                dropAllObjects(systemDataSource);
            }

            // clean up DefaultDB
            DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();

            if (defaultDataSource.isOfType(DatabaseSystem.H2)) {
                dropAllObjects(defaultDataSource);
                return;
            }

            if (defaultDataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
                deleteSchemas(defaultDataSource);
                createSchema(defaultDataSource, "public");
                return;
            }

            if (defaultDataSource.isOfType(DatabaseSystem.MSSQL)) {
                cleanupMSSQL(defaultDataSource);
                return;
            }

            throw new IllegalStateException("Missing cleanup logic for default datasource " + defaultDataSource);
        } finally {
            deleteCMSFolder();
            deleteDirigibleFolder();
        }
    }

    private void cleanupMSSQL(DirigibleDataSource dataSource) {
        LOGGER.info("Will cleanup MSSQL default datasource [{}]...", dataSource);
        String cleanUpSql = """
                /* 1. Break Foreign Key Dependencies */
                DECLARE @sql NVARCHAR(MAX) = N'';
                SELECT @sql += 'ALTER TABLE ' + QUOTENAME(s.name) + '.' + QUOTENAME(t.name) +\s
                               ' DROP CONSTRAINT ' + QUOTENAME(f.name) + ';'
                FROM sys.foreign_keys AS f
                INNER JOIN sys.tables AS t ON f.parent_object_id = t.object_id
                INNER JOIN sys.schemas AS s ON t.schema_id = s.schema_id
                WHERE t.is_ms_shipped = 0; -- Only target your tables
                EXEC sp_executesql @sql;

                /* 2. Drop User-Created Objects (Tables, Views, Procs, etc.) */
                SET @sql = N'';
                SELECT @sql += 'DROP ' +\s
                    CASE type\s
                        WHEN 'U' THEN 'TABLE '\s
                        WHEN 'V' THEN 'VIEW '\s
                        WHEN 'P' THEN 'PROCEDURE '\s
                        WHEN 'FN' THEN 'FUNCTION '\s
                        WHEN 'IF' THEN 'FUNCTION '\s
                        WHEN 'TF' THEN 'FUNCTION '\s
                        WHEN 'SO' THEN 'SEQUENCE '
                    END + QUOTENAME(s.name) + '.' + QUOTENAME(o.name) + ';'
                FROM sys.objects AS o
                INNER JOIN sys.schemas AS s ON o.schema_id = s.schema_id
                WHERE o.is_ms_shipped = 0 -- IMPORTANT: Avoid system objects
                  AND s.name NOT IN ('sys', 'information_schema')
                  AND o.type IN ('U', 'V', 'P', 'FN', 'IF', 'TF', 'SO');
                EXEC sp_executesql @sql;

                /* 3. Drop Custom Schemas (excluding dbo and system defaults) */
                SET @sql = N'';
                SELECT @sql += 'DROP SCHEMA ' + QUOTENAME(name) + ';'
                FROM sys.schemas
                WHERE name NOT IN ('dbo', 'guest', 'INFORMATION_SCHEMA', 'sys', 'db_owner',\s
                                  'db_accessadmin', 'db_securityadmin', 'db_ddladmin',\s
                                  'db_backupoperator', 'db_datareader', 'db_datawriter',\s
                                  'db_denydatareader', 'db_denydatawriter');
                EXEC sp_executesql @sql;
                """;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(cleanUpSql)) {
                preparedStatement.executeUpdate();
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to cleanup MSSQL datasource [{}] using sql:\n{}", dataSource, cleanUpSql, ex);
        }
    }

    private void dropAllObjects(DirigibleDataSource dataSource) {
        LOGGER.info("Will drop all objects from [{}]...", dataSource);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("DROP ALL OBJECTS")) {
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            LOGGER.warn("Failed to drop all objects from [{}]", dataSource, ex);
        }
    }

    private void clearEntityManagerCaches() {
        LOGGER.info("Clearing entity manager caches...");
        try {
            entityManagerFactory.getCache()
                                .evictAll();
        } catch (Exception ex) {
            LOGGER.warn("Failed to clear entity manager caches", ex);
        }
    }

    private void deleteSchemas(DirigibleDataSource dataSource) {
        try {
            Set<String> schemas = getSchemas(dataSource);
            schemas.remove("INFORMATION_SCHEMA");
            schemas.remove("information_schema");
            schemas.removeIf(s -> s.startsWith("pg_"));

            LOGGER.debug("Will drop schemas [{}] from data source [{}]", schemas, dataSource);
            schemas.forEach(schema -> deleteSchema(schema, dataSource));
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete schemas from [{}]", dataSource, ex);
        }
    }

    private Set<String> getSchemas(DirigibleDataSource dataSource) {
        try {
            if (dataSource.isOfType(DatabaseSystem.H2)) {
                return getSchemas(dataSource, "SHOW SCHEMAS");
            }

            if (dataSource.isOfType(DatabaseSystem.POSTGRESQL)) {
                return getSchemas(dataSource, "SELECT nspname FROM pg_catalog.pg_namespace");
            }

            if (dataSource.isOfType(DatabaseSystem.MSSQL)) {
                return getSchemas(dataSource, "SELECT name FROM sys.schemas");
            }

            throw new IllegalStateException("Missing implementation for getting schemas for datasource " + dataSource);

        } catch (SQLException ex) {
            try {
                return getSchemas(dataSource, "SELECT nspname FROM pg_catalog.pg_namespace");
            } catch (SQLException e) {
                e.addSuppressed(ex);
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
        LOGGER.info("Will drop schema [{}] from data source [{}]...", schema, dataSource);
        try (Connection connection = dataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
            String sql = dialect.drop()
                                .schema(schema)
                                .cascade(true)
                                .generate();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to drop schema [{}] from dataSource [{}] ", schema, dataSource, ex);
        }
    }

    private void createSchema(DirigibleDataSource dataSource, String schemaName) {
        LOGGER.info("Will create schema [{}] in [{}]...", schemaName, dataSource);
        try (Connection connection = dataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
            String sql = dialect.create()
                                .schema(schemaName)
                                .generate();
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to create schema [{}] in dataSource [{}] ", schemaName, dataSource, ex);
        }
    }

    public static void deleteDirigibleFolder() {
        String dirigibleFolder = DirigibleConfig.REPOSITORY_LOCAL_ROOT_FOLDER.getStringValue() + File.separator + "dirigible";
        String skippedDirPath = dirigibleFolder + File.separator + "repository" + File.separator + "index";
        LOGGER.info("Deleting dirigible folder [{}] by skipping [{}]...", dirigibleFolder, skippedDirPath);
        try {
            FileUtil.deleteFolder(dirigibleFolder, skippedDirPath);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete dirigible folder [{}] by skipping [{}]", dirigibleFolder, skippedDirPath, ex);
        }
    }

    public static void deleteCMSFolder() {
        String cmsFolder = DirigibleConfig.CMS_INTERNAL_ROOT_FOLDER.getStringValue();
        LOGGER.info("Deleting CMS folder [{}]...", cmsFolder);
        try {
            FileUtil.deleteFolder(cmsFolder);
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to delete CMS folder [{}]", cmsFolder, ex);
        }
    }
}
