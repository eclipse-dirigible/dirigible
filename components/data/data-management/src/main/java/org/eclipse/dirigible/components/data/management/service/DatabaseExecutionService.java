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


import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.data.sources.domain.DataSource;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.sources.service.DataSourceService;
import org.eclipse.dirigible.components.database.helpers.DatabaseErrorHelper;
import org.eclipse.dirigible.components.database.helpers.DatabaseQueryHelper;
import org.eclipse.dirigible.components.database.helpers.DatabaseResultSetHelper;
import org.eclipse.dirigible.components.database.helpers.DatabaseQueryHelper.RequestExecutionCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The Class DataSourceMetadataService.
 */
@Service
public class DatabaseExecutionService {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseExecutionService.class);

    /** The Constant CREATE_PROCEDURE. */
    private static final String CREATE_PROCEDURE = "CREATE PROCEDURE";

    /** The Constant SCRIPT_DELIMITER. */
    private static final String SCRIPT_DELIMITER = ";";

    /** The Constant PROCEDURE_DELIMITER. */
    private static final String PROCEDURE_DELIMITER = "--";

    /** The limited. */
    private final boolean LIMITED = true;

    /** The data sources manager. */
    private final DataSourcesManager datasourceManager;

    /** The data sources service. */
    private final DataSourceService datasourceService;

    /**
     * Instantiates a new data source endpoint.
     *
     * @param datasourceManager the datasource manager
     * @param datasourceService the datasource service
     */
    @Autowired
    public DatabaseExecutionService(DataSourcesManager datasourceManager, DataSourceService datasourceService) {
        this.datasourceManager = datasourceManager;
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
     * Execute query.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param isJson the is json
     * @param isCsv the is csv
     * @param output the output
     */
    public void executeQuery(String datasource, String sql, boolean isJson, boolean isCsv, OutputStream output) {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            executeStatement(dataSource, sql, true, isJson, isCsv, true, output);
        }
    }

    /**
     * Execute update.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param isJson the is json
     * @param isCsv the is csv
     * @param output the output
     */
    public void executeUpdate(String datasource, String sql, boolean isJson, boolean isCsv, OutputStream output) {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            executeStatement(dataSource, sql, false, isJson, isCsv, true, output);
        }
    }

    /**
     * Execute update.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param isJson the is json
     * @param isCsv the is csv
     * @param output the output
     */
    public void executeProcedure(String datasource, String sql, boolean isJson, boolean isCsv, OutputStream output) {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            executeProcedure(dataSource, sql, isJson, isCsv, output);
        }
    }

    /**
     * Execute.
     *
     * @param datasource the datasource
     * @param sql the sql
     * @param isJson the is json
     * @param isCsv the is csv
     * @param output the output
     */
    public void execute(String datasource, String sql, boolean isJson, boolean isCsv, OutputStream output) {
        javax.sql.DataSource dataSource = datasourceManager.getDataSource(datasource);
        if (dataSource != null) {
            executeStatement(dataSource, sql, true, isJson, isCsv, true, output);
        }
    }

    /**
     * Execute statement.
     *
     * @param dataSource the data source
     * @param sql the sql
     * @param isQuery the is query
     * @param isJson the is json
     * @param isCsv the is csv
     * @param limited the limited
     * @param output the output
     */
    public void executeStatement(javax.sql.DataSource dataSource, String sql, boolean isQuery, boolean isJson, boolean isCsv,
            boolean limited, OutputStream output) {

        if ((sql == null) || (sql.length() == 0)) {
            return;
        }

        List<String> errors = new ArrayList<String>();

        StringTokenizer tokenizer = new StringTokenizer(sql, getDelimiter(sql));
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if ("".equals(line.trim())) {
                continue;
            }

            try (Connection connection = dataSource.getConnection()) {
                DatabaseQueryHelper.executeSingleStatement(connection, line, isQuery, new RequestExecutionCallback() {
                    @Override
                    public void updateDone(int recordsCount) {}

                    @Override
                    public void queryDone(ResultSet rs) {
                        try {
                            if (isJson) {
                                DatabaseResultSetHelper.toJson(rs, limited, true, output);
                            } else if (isCsv) {
                                DatabaseResultSetHelper.toCsv(rs, limited, false, output);
                            } else {
                                DatabaseResultSetHelper.print(rs, limited, output);
                            }
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(e.getMessage(), e);
                            }
                            errors.add(e.getMessage());
                        }
                    }

                    @Override
                    public void error(Throwable t) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(t.getMessage(), t);
                        }
                        errors.add(t.getMessage());
                    }
                });
            } catch (SQLException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(DatabaseErrorHelper.print(String.join("\n", errors)));
        }
    }

    /**
     * Execute procedure.
     *
     * @param dataSource the data source
     * @param sql the sql
     * @param isJson the is json
     * @param isCsv the is csv
     * @param output the output
     */
    private void executeProcedure(javax.sql.DataSource dataSource, String sql, boolean isJson, boolean isCsv, OutputStream output) {

        if ((sql == null) || (sql.length() == 0)) {
            return;
        }

        List<String> errors = new ArrayList<String>();

        StringTokenizer tokenizer = new StringTokenizer(sql, getDelimiter(sql));
        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken();
            if ("".equals(line.trim())) {
                continue;
            }

            try (Connection connection = dataSource.getConnection()) {
                DatabaseQueryHelper.executeSingleProcedure(connection, line, new RequestExecutionCallback() {

                    @Override
                    public void updateDone(int recordsCount) {}

                    @Override
                    public void queryDone(ResultSet rs) {
                        try {
                            if (isJson) {
                                DatabaseResultSetHelper.toJson(rs, LIMITED, true, output);
                            } else if (isCsv) {
                                DatabaseResultSetHelper.toCsv(rs, LIMITED, false, output);
                            } else {
                                DatabaseResultSetHelper.print(rs, LIMITED, output);
                            }
                        } catch (Exception e) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(e.getMessage(), e);
                            }
                            errors.add(e.getMessage());
                        }
                    }

                    @Override
                    public void error(Throwable t) {
                        if (logger.isWarnEnabled()) {
                            logger.warn(t.getMessage(), t);
                        }
                        errors.add(t.getMessage());
                    }
                });
            } catch (SQLException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException(DatabaseErrorHelper.print(String.join("\n", errors)));
        }
    }

    /**
     * Gets the delimiter.
     *
     * @param sql the sql
     * @return the delimiter
     */
    private String getDelimiter(String sql) {
        if (StringUtils.containsIgnoreCase(sql, CREATE_PROCEDURE)) {
            return PROCEDURE_DELIMITER;
        }
        return SCRIPT_DELIMITER;
    }
}
