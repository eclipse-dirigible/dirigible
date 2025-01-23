/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.service;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.StringTokenizer;

/**
 * The Class CustomDataSourcesService.
 */
@Service
public class CustomDataSourcesService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CustomDataSourcesService.class);

    /** The data source service. */
    @Autowired
    private DataSourceService dataSourceService;

    /**
     * Initialize.
     */
    public void initialize() {
        String customDataSourcesList = Configuration.get("DIRIGIBLE_DATABASE_CUSTOM_DATASOURCES");
        if ((customDataSourcesList != null) && !"".equals(customDataSourcesList)) {
            logger.trace("Custom datasources list: [{}]", customDataSourcesList);
            StringTokenizer tokens = new StringTokenizer(customDataSourcesList, ",");
            while (tokens.hasMoreTokens()) {
                String name = tokens.nextToken();
                logger.info("Initializing a custom datasource with name [{}]", name);
                saveDataSource(name);
            }
        } else {
            logger.trace("No custom datasources configured");
        }
        logger.debug("[{}] module initialized.", this.getClass()
                                                     .getCanonicalName());
    }

    /**
     * Save data source model.
     *
     * @param name the name
     */
    private void saveDataSource(String name) {
        String databaseDriver = getRequiredParameter(name, "DRIVER");
        String databaseUrl = getRequiredParameter(name, "URL");
        String databaseUsername = getOptionalParameter(name, "USERNAME");
        String databasePassword = getOptionalParameter(name, "PASSWORD");
        String databaseSchema = getOptionalParameter(name, "SCHEMA");

        org.eclipse.dirigible.components.data.sources.domain.DataSource ds =
                new org.eclipse.dirigible.components.data.sources.domain.DataSource("ENV_" + name, name, null, databaseDriver, databaseUrl,
                        databaseUsername, databasePassword);
        ds.setSchema(databaseSchema);
        ds.updateKey();
        ds.setLifecycle(ArtefactLifecycle.NEW);
        DataSource maybe = dataSourceService.findByKey(ds.getKey());
        if (maybe != null) {
            dataSourceService.delete(maybe);
        }
        dataSourceService.save(ds);
    }

    private String getRequiredParameter(String dataSourceName, String suffix) {
        String configName = createConfigName(dataSourceName, suffix);
        String value = Configuration.get(configName);
        if (null == value || value.trim()
                                  .isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration parameter [" + configName + "] for data source ["
                    + dataSourceName + "]. The value is: " + value);
        }
        return value;
    }

    private String createConfigName(String dataSourceName, String suffix) {
        return dataSourceName + "_" + suffix;
    }

    private String getOptionalParameter(String dataSourceName, String suffix) {
        String configName = createConfigName(dataSourceName, suffix);
        String value = Configuration.get(configName);
        if (null == value || value.trim()
                                  .isEmpty()) {
            logger.info("Optional parameter [{}] for data source [{}] is missing. The value is: [{}]", configName, dataSourceName, value);
        }
        return value;
    }

}
