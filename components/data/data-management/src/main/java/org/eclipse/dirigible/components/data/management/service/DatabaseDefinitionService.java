/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.management.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.data.management.load.DataSourceMetadataLoader;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The Class DataSourceMetadataService.
 */
@Service
public class DatabaseDefinitionService {

    /** The data sources manager. */
    private final DataSourcesManager datasourceManager;

    /** The data loader. */
    private final DataSourceMetadataLoader datasourceMetadataLoader;

    /** The data sources service. */
    private final DataSourceService datasourceService;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceManager the datasource manager
     * @param datasourceMetadataLoader the datasource metadata loader
     * @param datasourceService the datasource service
     */
    @Autowired
    public DatabaseDefinitionService(DataSourcesManager datasourceManager, DataSourceMetadataLoader datasourceMetadataLoader,
            DataSourceService datasourceService) {
        this.datasourceManager = datasourceManager;
        this.datasourceMetadataLoader = datasourceMetadataLoader;
        this.datasourceService = datasourceService;
    }

    /**
     * Gets the data sources.
     *
     * @return the data sources
     */
    public Set<String> getDataSourcesNames() {
        return datasourceService.getAll()
                                .stream()
                                .map(DataSource::getName)
                                .collect(Collectors.toSet());
    }

    /**
     * Gets the schemas.
     *
     * @param datasource the datasource
     * @return the schemas
     * @throws SQLException the SQL exception
     */
    public Set<String> getSchemasNames(String datasource) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        return datasourceMetadataLoader.getSchemas(dataSource);
    }

    /**
     * Gets the schema metadata.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @return the schema metadata
     * @throws SQLException the SQL exception
     */
    public String loadSchemaMetadata(String datasource, String schema) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        List<Table> model = datasourceMetadataLoader.loadSchemaMetadata(schema, dataSource);
        model.forEach(m -> {
            m.setType(m.getKind());
            if (ISqlKeywords.METADATA_TABLE_STRUCTURES.contains(m.getType())) {
                m.setType(ISqlKeywords.METADATA_TABLE);
            }
        });

        JsonArray structureArray = new JsonArray();
        for (Table m : model) {
            structureArray.add(JsonHelper.fromJson(JsonHelper.toJson(m), JsonElement.class));
        }

        JsonObject schemaObject = new JsonObject();
        schemaObject.add("structures", structureArray);

        JsonObject json = new JsonObject();
        json.add("schema", schemaObject);

        return JsonHelper.toJson(json);
    }

    /**
     * Gets the structure metadata.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param structure the structure
     * @return the structure metadata
     * @throws SQLException the SQL exception
     */
    public String loadStructureMetadata(String datasource, String schema, String structure) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        Table model = datasourceMetadataLoader.loadTableMetadata(schema, structure, dataSource);
        if (model != null) {
            return JsonHelper.toJson(model);
        }
        return null;
    }

}
