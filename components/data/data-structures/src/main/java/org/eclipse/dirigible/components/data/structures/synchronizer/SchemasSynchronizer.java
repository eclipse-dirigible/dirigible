/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.data.sources.config.DefaultDataSourceName;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.*;
import org.eclipse.dirigible.components.data.structures.service.SchemaService;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.eclipse.dirigible.components.data.structures.synchronizer.schema.SchemaCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.schema.SchemaUpdateProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * The Class SchemasSynchronizer.
 */
@Component
@Order(SynchronizersOrder.SCHEMA)
public class SchemasSynchronizer extends MultitenantBaseSynchronizer<Schema, Long> {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(SchemasSynchronizer.class);

    /**
     * The Constant FILE_EXTENSION_SCHEMA.
     */
    private static final String FILE_EXTENSION_SCHEMA = ".schema";

    /**
     * The schema service.
     */
    private final SchemaService schemaService;

    /**
     * The table service.
     */
    private final TableService tableService;

    /**
     * The view service.
     */
    private final ViewService viewService;
    private final String defaultDataSourceName;

    /**
     * The datasources manager.
     */
    private final DataSourcesManager datasourcesManager;

    /**
     * The synchronization callback.
     */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new schema synchronizer.
     *
     * @param schemaService the schema service
     * @param datasourcesManager the datasources manager
     * @param tableService the table service
     * @param viewService the view service
     */
    @Autowired
    public SchemasSynchronizer(SchemaService schemaService, DataSourcesManager datasourcesManager, TableService tableService,
            ViewService viewService, @DefaultDataSourceName String defaultDataSourceName) {
        this.schemaService = schemaService;
        this.datasourcesManager = datasourcesManager;
        this.tableService = tableService;
        this.viewService = viewService;
        this.defaultDataSourceName = defaultDataSourceName;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Schema.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Schema> parseImpl(String location, byte[] content) throws ParseException {
        final Schema schema = parseSchema(location, new String(content, StandardCharsets.UTF_8));
        Configuration.configureObject(schema);
        schema.setLocation(location);
        if (schema.getName() == null) {
            schema.setName("PUBLIC");
        }
        schema.setType(Schema.ARTEFACT_TYPE);
        schema.updateKey();

        schema.getTables()
              .forEach(t -> {
                  t.setSchemaReference(schema);
                  // t.setSchema(schema.getName());
                  t.setConstraints(new TableConstraints(t));
                  TablesSynchronizer.assignParent(t);
              });
        schema.getViews()
              .forEach(v -> v.setSchemaReference(schema));

        try {
            Schema maybe = getService().findByKey(schema.getKey());
            if (maybe != null) {
                schema.setId(maybe.getId());
                schema.getTables()
                      .forEach(t -> {
                          Table m = getTableService().findByKey(schema.constructKey(Table.ARTEFACT_TYPE, location, t.getName()));
                          if (m != null) {
                              t.setId(m.getId());
                              TablesSynchronizer.reassignIds(t, m);
                          }
                      });
                schema.getViews()
                      .forEach(v -> {
                          View m = getViewService().findByKey(schema.constructKey(View.ARTEFACT_TYPE, location, v.getName()));
                          if (m != null) {
                              v.setId(m.getId());
                          }
                      });
            }
            Schema result = getService().save(schema);
            return List.of(result);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("schema: {}", schema);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
     * Parses the schema.
     *
     * @param location the location
     * @param content the content
     * @return the schema
     */
    public Schema parseSchema(String location, String content) {
        Schema result = new Schema();

        JsonElement root = GsonHelper.parseJson(content);
        JsonObject rootJsonObject = root.getAsJsonObject();
        JsonElement schemaElement = rootJsonObject.get("schema");
        JsonObject schemaJsonObject = schemaElement.getAsJsonObject();
        JsonElement strs = schemaJsonObject.get("structures");
        JsonArray structures = strs.getAsJsonArray();

        JsonElement datasourceJsonElement = rootJsonObject.get("datasource");

        String dataSource = datasourceJsonElement != null ? datasourceJsonElement.getAsString() : defaultDataSourceName;
        result.setDataSource(dataSource);
        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.get(i)
                                             .getAsJsonObject();
            String type = structure.get("type")
                                   .getAsString();
            if ("table".equalsIgnoreCase(type)) {
                Table table = new Table();
                setTableAttributes(location, result, structure, type, table);
                result.getTables()
                      .add(table);
            } else if ("view".equalsIgnoreCase(type)) {
                View view = new View();
                setViewAttributes(location, result, structure, type, view);
                result.getViews()
                      .add(view);
            } else if ("foreignKey".equalsIgnoreCase(type)) {
                // skip for now
            } else {
                throw new IllegalArgumentException(format("Unknown data structure type [{0}] loaded from schema [{1}]", type, location));
            }
        }
        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.get(i)
                                             .getAsJsonObject();
            String type = structure.get("type")
                                   .getAsString();
            if ("foreignKey".equals(type)) {
                TableConstraintForeignKey foreignKey = new TableConstraintForeignKey();
                foreignKey.setName(structure.get("name")
                                            .getAsString());
                foreignKey.setColumns(structure.get("columns")
                                               .getAsString()
                                               .split(","));
                foreignKey.setReferencedTable(structure.get("referencedTable")
                                                       .getAsString());
                foreignKey.setReferencedColumns(structure.get("referencedColumns")
                                                         .getAsString()
                                                         .split(","));
                String tableName = structure.get("table")
                                            .getAsString();
                for (Table table : result.getTables()) {
                    if (table.getName()
                             .equals(tableName)) {
                        // add the foreign key
                        List<TableConstraintForeignKey> list = new ArrayList<TableConstraintForeignKey>();
                        if (table.getConstraints()
                                 .getForeignKeys() != null) {
                            list.addAll(table.getConstraints()
                                             .getForeignKeys());
                        }
                        list.add(foreignKey);
                        table.getConstraints()
                             .getForeignKeys()
                             .addAll(list);
                        // add the dependency for the topological sorting later
                        table.addDependency(location, foreignKey.getReferencedTable(), "TABLE");
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Sets the table attributes.
     *
     * @param location the location
     * @param result the result
     * @param structure the structure
     * @param type the type
     * @param table the table
     */
    private static void setTableAttributes(String location, Schema result, JsonObject structure, String type, Table table) {
        table.setLocation(location);
        table.setName(structure.get("name")
                               .getAsString());
        table.setKind(type);
        table.setType(Table.ARTEFACT_TYPE);
        table.updateKey();
        JsonElement columnElement = structure.get("columns");
        if (columnElement.isJsonObject()) {
            JsonObject column = columnElement.getAsJsonObject();
            TableColumn columnModel = new TableColumn();
            setColumnAttributes(column, columnModel);
            columnModel.setTable(table);
            table.getColumns()
                 .add(columnModel);
        } else if (columnElement.isJsonArray()) {
            JsonArray columns = columnElement.getAsJsonArray();
            for (int j = 0; j < columns.size(); j++) {
                JsonObject column = columns.get(j)
                                           .getAsJsonObject();
                TableColumn columnModel = new TableColumn();
                setColumnAttributes(column, columnModel);
                columnModel.setTable(table);
                table.getColumns()
                     .add(columnModel);
            }
        } else {
            throw new IllegalArgumentException(
                    format("Error in parsing columns of table [{0}] in schema [{1}]", table.getName(), location));
        }
    }

    /**
     * Sets the column attributes.
     *
     * @param column the column
     * @param columnModel the column model
     */
    private static void setColumnAttributes(JsonObject column, TableColumn columnModel) {
        String nameValue = getJsonElementValue(column, "name", "unknown");
        columnModel.setName(nameValue);

        String typeValue = getJsonElementValue(column, "type", "unknown");
        columnModel.setType(typeValue);

        String lengthValue = getJsonElementValue(column, "length", null);
        columnModel.setLength(lengthValue);

        columnModel.setPrimaryKey(column.get("primaryKey") != null && !column.get("primaryKey")
                                                                             .isJsonNull()
                && column.get("primaryKey")
                         .getAsBoolean());
        columnModel.setUnique(column.get("unique") != null && !column.get("unique")
                                                                     .isJsonNull()
                && column.get("unique")
                         .getAsBoolean());
        columnModel.setNullable(column.get("nullable") != null && !column.get("nullable")
                                                                         .isJsonNull()
                && column.get("nullable")
                         .getAsBoolean());

        String defaultValue = getJsonElementValue(column, "defaultValue", null);
        columnModel.setDefaultValue(defaultValue);

        String scaleValue = getJsonElementValue(column, "scale", null);
        columnModel.setScale(scaleValue);

        String precisionValue = getJsonElementValue(column, "precision", null);
        columnModel.setPrecision(precisionValue);
    }

    private static String getJsonElementValue(JsonObject jsonObject, String memberName, String defaultValue) {
        JsonElement jsonElement = jsonObject.get(memberName);
        return (jsonElement != null && !jsonElement.isJsonNull()) ? jsonElement.getAsString() : defaultValue;
    }

    /**
     * Sets the view attributes.
     *
     * @param location the location
     * @param result the result
     * @param structure the structure
     * @param type the type
     * @param view the view
     */
    private static void setViewAttributes(String location, Schema result, JsonObject structure, String type, View view) {
        view.setLocation(location);
        view.setName(structure.get("name")
                              .getAsString());
        view.setKind(type);
        view.setType(View.ARTEFACT_TYPE);
        JsonElement columns = structure.get("columns");
        view.setQuery(columns.getAsJsonArray()
                             .get(0)
                             .getAsJsonObject()
                             .get("query")
                             .getAsString());
        view.updateKey();
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Schema, Long> getService() {
        return schemaService;
    }

    /**
     * Gets the table service.
     *
     * @return the table service
     */
    public TableService getTableService() {
        return tableService;
    }

    /**
     * Gets the view service.
     *
     * @return the view service
     */
    public ViewService getViewService() {
        return viewService;
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Schema> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Schema artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete impl.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Schema> wrapper, ArtefactPhase flow) {
        Schema schema = wrapper.getArtefact();

        DataSource dataSource = null;
        try {
            String dataSourceName = schema.getDatasource();
            if (dataSourceName == null || "".equals(dataSourceName.trim()) || "DefaultDB".equals(dataSourceName)) {
                dataSource = datasourcesManager.getDefaultDataSource();
            } else {
                dataSource = datasourcesManager.getDataSource(dataSourceName);
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }

            if (dataSource == null) {
                if (ArtefactLifecycle.FAILED.equals(schema.getLifecycle())) {
                    callback.addError(e.getMessage());
                    callback.registerState(this, wrapper, ArtefactLifecycle.FATAL, e);
                    return true;
                }
            }
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            switch (flow) {
                case CREATE:
                    if (schema.getLifecycle()
                              .equals(ArtefactLifecycle.NEW)) {
                        try {
                            executeSchemaCreate(connection, schema);
                            callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                        } catch (Exception e) {
                            callback.registerState(this, wrapper, ArtefactLifecycle.CREATED, e);
                        }
                    }
                    break;
                case UPDATE:
                    if (schema.getLifecycle()
                              .equals(ArtefactLifecycle.MODIFIED)) {
                        executeSchemaUpdate(connection, schema);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (schema.getLifecycle()
                              .equals(ArtefactLifecycle.MODIFIED)) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(schema.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(schema.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(schema.getLifecycle())) {
                        executeSchemaDrop(connection, schema);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                        break;
                    }
                case START:
                case STOP:
            }

            return true;

        } catch (SQLException e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    /**
     * Execute schema create.
     *
     * @param connection the connection
     * @param schemaModel the schema model
     * @throws SQLException the SQL exception
     */
    public void executeSchemaCreate(Connection connection, Schema schemaModel) throws SQLException {
        SchemaCreateProcessor.execute(connection, schemaModel);
    }

    /**
     * Execute schema update.
     *
     * @param connection the connection
     * @param schemaModel the schema model
     * @throws SQLException the SQL exception
     */
    public void executeSchemaUpdate(Connection connection, Schema schemaModel) throws SQLException {
        if (logger.isInfoEnabled()) {
            logger.info("Processing Update Schema: " + schemaModel.getName());
        }
        if (SqlFactory.getNative(connection)
                      .existsSchema(connection, schemaModel.getName())) {
            SchemaUpdateProcessor.execute(connection, schemaModel);
        } else {
            executeSchemaCreate(connection, schemaModel);
        }
    }

    /**
     * Execute schema drop.
     *
     * @param connection the connection
     * @param schemaModel the schema model
     * @throws SQLException the SQL exception
     */
    public void executeSchemaDrop(Connection connection, Schema schemaModel) throws SQLException {
        // SchemaDropProcessor.execute(connection, schemaModel);
    }

    /**
     * Cleanup.
     *
     * @param schema the schema
     */
    @Override
    public void cleanupImpl(Schema schema) {
        try {
            getService().delete(schema);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, schema, ArtefactLifecycle.DELETED, e);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_SCHEMA;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Schema.ARTEFACT_TYPE;
    }

}
