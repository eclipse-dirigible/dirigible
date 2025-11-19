/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.export.service;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.platform.WorkspaceFacade;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.management.load.DataSourceMetadataLoader;
import org.eclipse.dirigible.components.data.management.service.DatabaseDefinitionService;
import org.eclipse.dirigible.components.data.management.service.DatabaseExecutionService;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.transfer.service.DataTransferSchemaTopologyService;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.domain.Project;
import org.eclipse.dirigible.components.ide.workspace.domain.Workspace;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The Class DataExportService.
 */
@Service
public class DataExportService {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);

    /**
     * The Constant DEFAULT_WORKSPACE_NAME.
     */
    private static final String DEFAULT_WORKSPACE_NAME = "workspace";

    /**
     * The data sources manager.
     */
    private final DataSourcesManager datasourceManager;

    /**
     * The workspace service.
     */
    private final WorkspaceService workspaceService;

    /**
     * The database execution service.
     */
    private final DatabaseExecutionService databaseExecutionService;

    /**
     * The database execution service.
     */
    private final DatabaseDefinitionService databaseDefinitionService;

    /**
     * The data transfer schema topology service.
     */
    private final DataTransferSchemaTopologyService dataTransferSchemaTopologyService;

    /**
     * The data source metadata loader service.
     */
    private final DataSourceMetadataLoader dataSourceMetadataLoader;

    /**
     * Instantiates a new data export service.
     *
     * @param datasourceManager the datasource manager
     * @param workspaceService the workspace service
     * @param databaseExecutionService the database execution service
     * @param databaseDefinitionService the database definition service
     * @param dataTransferSchemaTopologyService the data transfer schema topology service
     * @param dataSourceMetadataLoader the data source metadata loader service
     */
    @Autowired
    public DataExportService(DataSourcesManager datasourceManager, WorkspaceService workspaceService,
            DatabaseExecutionService databaseExecutionService, DatabaseDefinitionService databaseDefinitionService,
            DataTransferSchemaTopologyService dataTransferSchemaTopologyService, DataSourceMetadataLoader dataSourceMetadataLoader) {
        this.datasourceManager = datasourceManager;
        this.workspaceService = workspaceService;
        this.databaseExecutionService = databaseExecutionService;
        this.databaseDefinitionService = databaseDefinitionService;
        this.dataTransferSchemaTopologyService = dataTransferSchemaTopologyService;
        this.dataSourceMetadataLoader = dataSourceMetadataLoader;
    }

    /**
     * Export schema in csvs.
     *
     * @param datasource the datasource
     * @param schema the schema
     */
    public void exportSchemaInCsvs(String datasource, String schema) {
        try {
            DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
            if (dataSource != null) {
                Workspace workspace;
                ArrayList<CsvFile> csvFiles = new ArrayList<>();

                String metadata = DatabaseMetadataHelper.getMetadataAsJson(dataSource);
                JsonElement database = GsonHelper.parseJson(metadata);
                JsonArray schemes = database.getAsJsonObject()
                                            .get("schemas")
                                            .getAsJsonArray();

                workspace = workspaceService.existsWorkspace(DEFAULT_WORKSPACE_NAME) ? WorkspaceFacade.getWorkspace(DEFAULT_WORKSPACE_NAME)
                        : WorkspaceFacade.createWorkspace(DEFAULT_WORKSPACE_NAME);

                Project project = workspace.createProject(datasource);

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
                        File file;
                        CsvFile csvFile = new CsvFile();

                        JsonObject table = tables.get(j)
                                                 .getAsJsonObject();
                        String artifact = table.get("name")
                                               .getAsString();
                        String sql = "SELECT * FROM \"" + schema + "\".\"" + artifact + "\"";
                        try {
                            sql = SqlDialectFactory.getDialect(dataSource)
                                                   .allQuery("\"" + schema + "\".\"" + artifact + "\"");

                            try (Connection connection = dataSource.getConnection()) {
                                String integerPrimaryKey = DatabaseExportService.getIntegerPrimaryKey(connection, artifact);
                                if (integerPrimaryKey != null) {
                                    sql += " ORDER BY \"" + integerPrimaryKey + "\"";
                                }
                            }
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }

                        StringWriter sw = new StringWriter();
                        OutputStream output;
                        try {
                            output = WriterOutputStream.builder()
                                                       .setWriter(sw)
                                                       .setCharset(StandardCharsets.UTF_8)
                                                       .get();
                        } catch (IOException e) {
                            throw new SQLException(e);
                        }
                        databaseExecutionService.executeStatement(dataSource, sql, true, false, true, false, output);
                        String tableExport = sw.toString();

                        file = project.createFile(schema.toLowerCase() + "." + artifact.toLowerCase() + ".csv", tableExport.getBytes());

                        setCsvFileFields(csvFile, schema, artifact, file.getProjectPath());
                        csvFiles.add(csvFile);
                    }
                }
                JsonObject csvimContent = transformCsvFilesToJson(csvFiles);

                project.createFile(schema + ".csvim", csvimContent.toString()
                                                                  .getBytes());

                logger.info(format("Created requested files in Project [{0}] in Workspace [{1}]", project.getName(), workspace.getName()));
            }
        } catch (SQLException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Export metadata as project.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @return the workspace path of the file
     * @throws SQLException the SQL exception
     */
    public String exportMetadataAsProject(String datasource, String schema) throws SQLException {
        String schemaMetadata = databaseDefinitionService.loadSchemaMetadata(datasource, schema);
        Workspace workspace;
        Project project;
        File file;

        workspace = workspaceService.existsWorkspace(DEFAULT_WORKSPACE_NAME) ? WorkspaceFacade.getWorkspace(DEFAULT_WORKSPACE_NAME)
                : WorkspaceFacade.createWorkspace(DEFAULT_WORKSPACE_NAME);

        project = workspace.createProject(datasource);
        file = project.createFile(datasource.toLowerCase() + "_" + schema.toLowerCase() + ".schema", schemaMetadata.getBytes());
        logger.info(
                format("Created file [{0}] in Project [{1}] in Workspace [{2}]", file.getName(), project.getName(), workspace.getName()));

        return file.getWorkspacePath();
    }

    /**
     * Export metadata as project.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @return the workspace path of the file
     * @throws SQLException the SQL exception
     */
    public String exportSchemaTopology(String datasource, String schema) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            List<String> sorted = dataTransferSchemaTopologyService.sortTopologically(dataSource, schema);
            return sorted.stream()
                         .collect(Collectors.joining("\n"));
        }
        return "DataSource does not exist: " + datasource;
    }

    /**
     * Export schema as model.
     *
     * @param datasource the datasource
     * @param schema the schema
     */

    public void exportSchemaAsModel(String datasource, String schema) {
        try {
            javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);

            List<Table> model = dataSourceMetadataLoader.loadSchemaMetadata(schema, dataSource);
            model.forEach(m -> {
                m.setType(m.getKind());
                if (ISqlKeywords.METADATA_TABLE_STRUCTURES.contains(m.getType())) {
                    m.setType(ISqlKeywords.METADATA_TABLE);
                }
            });

            JsonArray entitiesArray = new JsonArray();
            JsonArray perspectivesArray = new JsonArray();
            JsonArray navigationsArray = new JsonArray();

            JsonObject modelObject = new JsonObject();
            modelObject.add("entities", entitiesArray);
            modelObject.add("perspectives", perspectivesArray);
            modelObject.add("navigations", navigationsArray);

            JsonObject schemaModel = new JsonObject();
            schemaModel.add("model", modelObject);

            if (dataSource != null) {
                for (Table table : model) {
                    addTableMetadataInModel(table, entitiesArray);
                }
            }

            Workspace workspace;

            workspace = workspaceService.existsWorkspace(DEFAULT_WORKSPACE_NAME) ? WorkspaceFacade.getWorkspace(DEFAULT_WORKSPACE_NAME)
                    : WorkspaceFacade.createWorkspace(DEFAULT_WORKSPACE_NAME);

            Project project = workspace.createProject(datasource);

            project.createFile(datasource.toLowerCase() + "_" + schema.toLowerCase() + ".model", schemaModel.toString()
                                                                                                            .getBytes());

            logger.info(format("Created requested files in Project [{0}] in Workspace [{1}]", project.getName(), workspace.getName()));
        } catch (SQLException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Adds the table metadata in model.
     *
     * @param table the table
     * @param entitiesArray the entities array
     */
    private void addTableMetadataInModel(Table table, JsonArray entitiesArray) {
        JsonObject tableObject = new JsonObject();
        JsonArray tableColumns = new JsonArray();

        for (TableColumn column : table.getColumns()) {
            JsonObject columnObject = populateColumnData(column);
            tableColumns.add(columnObject);
        }

        tableObject.add("properties", tableColumns);
        populateTableProperties(tableObject, table);

        entitiesArray.add(tableObject);
    }

    /**
     * Populate table properties.
     *
     * @param tableObject the table object
     * @param table the table
     */
    private void populateTableProperties(JsonObject tableObject, Table table) {
        tableObject.addProperty("caption", "Manage entity " + table.getName());
        tableObject.addProperty("dataCount", "SELECT COUNT(*) AS COUNT FROM \"${tablePrefix}" + table.getName() + "\"");
        tableObject.addProperty("dataName", table.getName());
        tableObject.addProperty("dataQuery", "");
        tableObject.addProperty("icon", "cubes");
        tableObject.addProperty("layoutType", "MANAGE");
        tableObject.addProperty("menuIndex", table.getName()
                                                  .toLowerCase());
        tableObject.addProperty("menuKey", table.getName()
                                                .toLowerCase());
        tableObject.addProperty("menuLabel", table.getName()
                                                  .toLowerCase());
        tableObject.addProperty("name", table.getName()
                                             .toLowerCase());
        tableObject.addProperty("navigationPath", "/Home");
        tableObject.addProperty("perspectiveIcon", "/services/web/resources/unicons/arrows-resize-v.svg"); // should not be hardcoded
        tableObject.addProperty("perspectiveName", table.getName()
                                                        .toLowerCase());
        tableObject.addProperty("perspectiveOrder", "");
        tableObject.addProperty("title", table.getName()
                                              .toUpperCase());
        tableObject.addProperty("tooltip", table.getName()
                                                .toUpperCase());
        tableObject.addProperty("type", "PRIMARY");
    }

    /**
     * Populate column data.
     *
     * @param column the column
     * @return the json object
     */
    private JsonObject populateColumnData(TableColumn column) {
        JsonObject columnObject = new JsonObject();
        columnObject.addProperty("calculatedPropertyExpressionCreate", "");
        columnObject.addProperty("calculatedPropertyExpressionUpdate", "");
        columnObject.addProperty("dataAutoIncrement", String.valueOf(column.isPrimaryKey()));
        columnObject.addProperty("dataLength", column.getLength());
        columnObject.addProperty("dataName", column.getName());
        columnObject.addProperty("dataNullable", String.valueOf(column.isNullable()));
        columnObject.addProperty("dataPrimaryKey", String.valueOf(column.isPrimaryKey()));
        columnObject.addProperty("dataType", column.getType());
        columnObject.addProperty("dataUnique", String.valueOf(column.isUnique()));
        columnObject.addProperty("isCalculatedProperty", "false");
        columnObject.addProperty("name", column.getName());
        columnObject.addProperty("widgetIsMajor", "true");
        columnObject.addProperty("widgetLength", column.getLength());
        columnObject.addProperty("widgetType", "TEXTBOX");

        return columnObject;
    }

    /**
     * Transform csv files to json.
     *
     * @param csvFiles the csvFiles
     * @return the transformed csvFiles to Json
     */
    private JsonObject transformCsvFilesToJson(ArrayList<CsvFile> csvFiles) {
        JsonObject csvimContent = new JsonObject();
        csvimContent.add("files", JsonHelper.toJsonTree(csvFiles));

        return csvimContent;
    }

    /**
     * Sets the csv file fields.
     *
     * @param csvFile the csvFile
     * @param schema the schema
     * @param artefact the artefact
     * @param filePath the filePath
     */
    private void setCsvFileFields(CsvFile csvFile, String schema, String artefact, String filePath) {
        csvFile.setTable(artefact);
        csvFile.setSchema(schema);
        csvFile.setFile(filePath);
        csvFile.setHeader(true);
        csvFile.setUseHeaderNames(true);
        csvFile.setDelimField(",");
        csvFile.setDelimEnclosing("\"");
        csvFile.setDistinguishEmptyFromNull(true);

    }
}
