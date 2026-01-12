/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
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
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.data.management.config.DatabaseMetadataCache;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.database.domain.DatabaseStructureTypes;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class DataSourceMetadataService.
 */
@Service
public class DatabaseMetadataService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataService.class);

    /** The data sources manager. */
    private final DataSourcesManager datasourceManager;

    /** The data sources service. */
    private final DataSourceService datasourceService;

    /** The cache. */
    private final DatabaseMetadataCache cache;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceManager the datasource manager
     * @param datasourceService the datasource service
     * @param cache the cache
     */
    @Autowired
    public DatabaseMetadataService(DataSourcesManager datasourceManager, DataSourceService datasourceService, DatabaseMetadataCache cache) {
        this.datasourceManager = datasourceManager;
        this.datasourceService = datasourceService;
        this.cache = cache;
    }

    /**
     * Gets the data sources.
     *
     * @return the data sources
     */
    public List<String> getDataSourcesNames() {
        return datasourceService.getAll()
                                .stream()
                                .map(DataSource::getName)
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .collect(Collectors.toList());
    }

    /**
     * Gets the structures.
     *
     * @param datasource the datasource
     * @return the structures
     * @throws SQLException the SQL exception
     */
    public String getDataSourceMetadata(String datasource) throws SQLException {
        String key = "DATASOURCE_" + datasource;
        String metadata = cache.get(key);
        if (metadata == null) {
            javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
            metadata = DatabaseMetadataHelper.getMetadataAsJson(dataSource);
            cache.put(key, metadata);
        }
        return metadata;
    }

    /**
     * Exists the structures.
     *
     * @param datasource the datasource
     * @return the structures
     * @throws SQLException the SQL exception
     */
    public boolean existsDataSourceMetadata(String datasource) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        return dataSource != null;
    }

    /**
     * Gets the schema metadata.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @return the schema metadata
     * @throws SQLException the SQL exception
     */
    public String getSchemaMetadata(String datasource, String schema) throws SQLException {
        String key = "DATASOURCE_" + datasource + "_SCHEMA_" + schema;
        String metadata = cache.get(key);
        if (metadata == null) {
            javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
            metadata = DatabaseMetadataHelper.getSchemaMetadataAsJson(dataSource, schema);
            cache.put(key, metadata);
        }
        return metadata;
    }

    /**
     * Invalidate cache.
     */
    public void invalidateCache() {
        cache.getInternalCache()
             .invalidateAll();
    }

    /**
     * Describe the requested artifact in JSON.
     *
     * @param datasource the requested datasource
     * @param schema the requested schema
     * @param artifact the requested artifact
     * @param kind the type of the artifact
     * @return the JSON representation
     * @throws SQLException in case of an error
     */
    public String getStructureMetadata(String datasource, String schema, String artifact, String kind) throws SQLException {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        String metadata = null;
        if (kind == null) {
            kind = DatabaseStructureTypes.TABLE.name();
        }
        if (artifact != null && !artifact.trim()
                                         .isEmpty()) {
            DatabaseStructureTypes type = DatabaseStructureTypes.TABLE;
            try {
                type = DatabaseStructureTypes.valueOf(kind);
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Kind is unknown", e);
                }
            }
            switch (type) {
                case PROCEDURE:
                    metadata = DatabaseMetadataHelper.getProcedureMetadataAsJson(dataSource, schema, artifact);
                    break;
                case FUNCTION:
                    metadata = DatabaseMetadataHelper.getFunctionMetadataAsJson(dataSource, schema, artifact);
                    break;
                default:
                    metadata = DatabaseMetadataHelper.getTableMetadataAsJson(dataSource, schema, artifact); // TABLE, VIEW
            }
        }
        return metadata;
    }

}
