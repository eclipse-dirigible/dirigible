/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DirigibleCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirigibleCleaner.class);

    private final DataSourcesManager dataSourcesManager;
    private final IRepository dirigibleRepo;

    DirigibleCleaner(DataSourcesManager dataSourcesManager, IRepository dirigibleRepo) {
        this.dataSourcesManager = dataSourcesManager;
        this.dirigibleRepo = dirigibleRepo;
    }

    public void clean() {
        try {
            deleteDatabases();
            deleteCMSFolderFiles();
            unpublishResources();
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to cleanup resources", ex);
        }
    }

    private void deleteDatabases() {
        LOGGER.info("Deleting Dirigible databases...");

        deleteDirigibleDBData();
        deleteH2Folder();

        LOGGER.info("Dirigible databases have been deleted...");
    }

    /**
     * Execute this before H2 folder deletion because it is in memory DB. Otherwise, will remain data in
     * memory.
     */
    private void deleteDirigibleDBData() {
        DirigibleDataSource defaultDataSource = dataSourcesManager.getDefaultDataSource();
        dropAllTablesInSchema(defaultDataSource);
        dropAllSequencesInSchema(defaultDataSource);

        DirigibleDataSource systemDataSource = dataSourcesManager.getSystemDataSource();
        deleteAllTablesDataInSchema(systemDataSource);
        dropAllTablesInSchema(systemDataSource, "QRTZ_");

        deleteSchemas(defaultDataSource);
    }

    private void dropAllSequencesInSchema(DirigibleDataSource dataSource) {
        List<String> sequences = getAllSequences(dataSource);
        LOGGER.info("Will drop [{}] sequences from data source [{}]. Sequences: {}", sequences.size(), dataSource, sequences);

        for (int idx = 0; idx < 4; idx++) {
            Iterator<String> iterator = sequences.iterator();
            while (iterator.hasNext()) {
                String sequence = iterator.next();
                try (Connection connection = dataSource.getConnection()) {
                    String sql = SqlDialectFactory.getDialect(dataSource)
                                                  .drop()
                                                  .sequence(sequence)
                                                  .build();
                    try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                        prepareStatement.executeUpdate();
                        LOGGER.info("Dropped sequence [{}]", sequence);
                        iterator.remove();
                    }
                } catch (SQLException ex) {
                    LOGGER.warn("Failed to drop sequence [{}] in data source [{}]", sequence, dataSource, ex);
                }
            }
        }
    }

    private List<String> getAllSequences(DataSource dataSource) {
        List<String> sequences = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement prepareStatement = connection.prepareStatement(
                        "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema='public' OR sequence_schema='PUBLIC'")) {
            ResultSet resultSet = prepareStatement.executeQuery();
            while (resultSet.next()) {
                sequences.add(resultSet.getString(1));
            }
            return sequences;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get all sequences in data source:" + dataSource, ex);
        }
    }

    private void dropAllTablesInSchema(DirigibleDataSource dataSource, String... skipTablePrefixes) {
        Set<String> tables = getAllTables(dataSource);
        for (String skipTablePrefix : skipTablePrefixes) {
            tables = tables.stream()
                           .filter(t -> !t.startsWith(skipTablePrefix))
                           .collect(Collectors.toSet());
        }

        LOGGER.info("Will drop [{}] tables from data source [{}]. Tables: {}", tables.size(), dataSource, tables);

        for (int idx = 0; idx < 4; idx++) { // execute it a few times due to constraint violations
            Iterator<String> iterator = tables.iterator();
            while (iterator.hasNext()) {
                String tableName = iterator.next();
                try (Connection connection = dataSource.getConnection()) {
                    String sql = SqlDialectFactory.getDialect(dataSource)
                                                  .drop()
                                                  .table(tableName)
                                                  .cascade(true)
                                                  .build();
                    try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                        prepareStatement.executeUpdate();
                        LOGGER.info("Dropped table [{}]", tableName);
                        iterator.remove();
                    }
                } catch (SQLException ex) {
                    LOGGER.warn("Failed to drop table [{}] in data source [{}]", tableName, dataSource, ex);
                }
            }
        }
    }

    private Set<String> getAllTables(DataSource dataSource) {
        Set<String> tables = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement prepareStatement = connection.prepareStatement(
                        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' OR TABLE_SCHEMA='public'")) {
            ResultSet resultSet = prepareStatement.executeQuery();
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
            return tables;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to get all tables in data source:" + dataSource, ex);
        }
    }

    private void deleteSchemas(DirigibleDataSource dataSource) {
        Set<String> schemas = getSchemas(dataSource);
        schemas.remove("PUBLIC");
        schemas.remove("public");
        schemas.remove("INFORMATION_SCHEMA");
        schemas.remove("information_schema");
        schemas.removeIf(s -> s.startsWith("pg_"));

        LOGGER.info("Will drop schemas [{}] from data source [{}]", schemas, dataSource);
        schemas.forEach(schema -> deleteSchema(schema, dataSource));
    }

    private Set<String> getSchemas(DataSource dataSource) {
        try {
            return getSchemas(dataSource, "SHOW SCHEMAS");
        } catch (SQLException ex) {
            try {
                return getSchemas(dataSource, "SELECT nspname FROM pg_catalog.pg_namespace");
            } catch (SQLException e) {
                IllegalStateException exc = new IllegalStateException("Failed to get all schemas from data source: " + dataSource, e);
                exc.addSuppressed(ex);
                throw exc;
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
            } catch (SQLException ex) {
                throw new IllegalStateException(
                        "Failed to drop schema [" + schema + "] from dataSource [" + dataSource + "] using sql: " + sql, ex);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to drop schema [" + schema + "] from dataSource [" + dataSource + "] ", ex);
        }
    }

    private void deleteAllTablesDataInSchema(DirigibleDataSource dataSource) {
        Set<String> tables = getAllTables(dataSource);

        for (int idx = 0; idx < 4; idx++) { // execute it a few times due to constraint violations
            Iterator<String> iterator = tables.iterator();
            while (iterator.hasNext()) {
                String table = iterator.next();
                try (Connection connection = dataSource.getConnection()) {
                    String sql = SqlDialectFactory.getDialect(dataSource)
                                                  .delete()
                                                  .from(table)
                                                  .build();
                    try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
                        int rowsAffected = prepareStatement.executeUpdate();
                        LOGGER.info("Deleted [{}] from table [{}]", rowsAffected, table);
                        iterator.remove();
                    }
                } catch (SQLException ex) {
                    LOGGER.warn("Failed to delete data from table [{}] in data source [{}]", table, dataSource, ex);
                }
            }
        }
    }

    private void deleteH2Folder() {
        String h2Folder = getDirigibleSubfolder("h2");
        FileUtil.deleteFolder(h2Folder);
    }

    private String getDirigibleSubfolder(String folder) {
        return System.getProperty("user.dir") + File.separator + "target" + File.separator + "dirigible" + File.separator + folder;
    }

    private void unpublishResources() throws IOException {
        LOGGER.info("Deleting all Dirigible project resources from the repository...");

        Set<String> userProjects = getUserProjects();
        deleteCurrentUserFolder();
        deleteDirigibleProjectsFromRegistry(userProjects);
        LOGGER.info("Dirigible project resources have been deleted.");
    }

    private Set<String> getUserProjects() throws IOException {
        return new HashSet<>(getUserProjectsFromWorkingDir());
    }

    private List<String> getUserProjectsFromWorkingDir() throws IOException {
        File usersRepoFolder = getUsersRepoFolder();
        if (usersRepoFolder.exists()) {
            List<Path> userProjectFiles = FileUtil.findFiles(usersRepoFolder, "project.json");
            return userProjectFiles.stream()
                                   .map(p -> p.toFile()
                                              .getParentFile()
                                              .getName())
                                   .toList();
        }
        LOGGER.info("Missing users repo folder [{}]", usersRepoFolder);
        return Collections.emptyList();
    }

    private File getUsersRepoFolder() {
        String repoBasePath = dirigibleRepo.getRepositoryPath();
        return new File(repoBasePath + File.separator + "users");
    }

    private void deleteCurrentUserFolder() {
        File usersFolder = getUsersRepoFolder();
        String currentUserFolder = usersFolder.getPath() + File.separator + DirigibleConfig.BASIC_ADMIN_USERNAME.getFromBase64Value();
        LOGGER.info("Will delete current user folder [{}]", currentUserFolder);
        FileUtil.deleteFolder(currentUserFolder);
    }

    private void deleteDirigibleProjectsFromRegistry(Set<String> userProjects) {
        String repoBasePath = dirigibleRepo.getRepositoryPath() + IRepositoryStructure.PATH_REGISTRY_PUBLIC + File.separator;
        LOGGER.info("Will delete user projects [{}] from the registry [{}]", userProjects, repoBasePath);
        userProjects.forEach(projectName -> {
            String projectPath = repoBasePath + projectName;
            FileUtil.deleteFolder(projectPath);
        });
    }

    private void deleteCMSFolderFiles() throws IOException {
        String cmdFolder = getDirigibleSubfolder("cms");
        FileUtil.findFiles(cmdFolder)
                .stream()
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
