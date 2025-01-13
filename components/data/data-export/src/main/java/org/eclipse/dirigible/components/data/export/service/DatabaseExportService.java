/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.data.management.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.data.management.service.DatabaseExecutionService;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The Class DataSourceMetadataService.
 */
@Service
public class DatabaseExportService {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExportService.class);

    /**
     * The data sources manager.
     */
    private final DataSourcesManager datasourceManager;

    /** The database execution service. */
    private final DatabaseExecutionService databaseExecutionService;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceManager the datasource manager
     * @param databaseExecutionService the database execution service
     */
    @Autowired
    public DatabaseExportService(DataSourcesManager datasourceManager, DatabaseExecutionService databaseExecutionService) {
        this.datasourceManager = datasourceManager;
        this.databaseExecutionService = databaseExecutionService;
    }

    /**
     * Export structure.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param structure the structure
     * @param output the output
     */
    public void exportStructure(String datasource, String schema, String structure, OutputStream output) {
        DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            String structureName = "\"" + schema + "\".\"" + structure + "\"";
            String sql = "SELECT * FROM " + structureName;
            try (Connection connection = dataSource.getConnection()) {
                ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
                sql = dialect.allQuery(structureName);
                if (dataSource.getDatabaseSystem()
                              .isMongoDB()) {
                    dialect.exportData(connection, structure, output);
                    return;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            databaseExecutionService.executeStatement(dataSource, sql, true, false, true, true, output);
        }
    }

    /**
     * Export structure.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param structure the structure
     * @return the string
     */
    public String structureExportType(String datasource, String schema, String structure) {
        DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            String productName = null;
            try {
                if (dataSource.getDatabaseSystem()
                              .isMongoDB()) {
                    return "json";
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            return "csv";
        }
        return "error";
    }

    /**
     * Export schema.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param output the output
     */
    public void exportSchema(String datasource, String schema, OutputStream output) {
        try {
            ZipOutputStream zipOutputStream = null;
            try {
                zipOutputStream = new ZipOutputStream(output);

                DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
                if (dataSource != null) {
                    String metadata = DatabaseMetadataHelper.getMetadataAsJson(dataSource);
                    JsonElement database = GsonHelper.parseJson(metadata);
                    JsonArray schemes = database.getAsJsonObject()
                                                .get("schemas")
                                                .getAsJsonArray();
                    for (int i = 0; i < schemes.size(); i++) {
                        JsonObject scheme = schemes.get(i)
                                                   .getAsJsonObject();
                        if (!scheme.get("name")
                                   .getAsString()
                                   .equalsIgnoreCase(schema)) {
                            continue;
                        }
                        JsonArray tables = scheme.get("tables")
                                                 .getAsJsonArray();
                        for (int j = 0; j < tables.size(); j++) {
                            JsonObject table = tables.get(j)
                                                     .getAsJsonObject();
                            String artifact = table.get("name")
                                                   .getAsString();
                            String artifactName = "\"" + schema + "\".\"" + artifact + "\"";
                            String sql = "SELECT * FROM " + artifactName;
                            try {
                                sql = SqlDialectFactory.getDialect(dataSource)
                                                       .allQuery(artifactName);
                            } catch (Exception e) {
                                logger.error(e.getMessage(), e);
                            }
                            ZipEntry zipEntry = new ZipEntry(schema + "." + artifact + ".csv");
                            zipOutputStream.putNextEntry(zipEntry);
                            databaseExecutionService.executeStatement(dataSource, sql, true, false, true, true, zipOutputStream);
                            zipOutputStream.closeEntry();
                        }
                    }
                }

            } finally {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
            }

        } catch (IOException | SQLException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

}
