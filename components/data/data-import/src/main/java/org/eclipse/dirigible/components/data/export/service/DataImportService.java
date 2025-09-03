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

import org.eclipse.dirigible.components.data.csvim.domain.CsvFile;
import org.eclipse.dirigible.components.data.csvim.processor.CsvimProcessor;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.dialects.SqlDialectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Connection;

/**
 * The Class DataImportService.
 */
@Service
public class DataImportService {

    /**
     * The data sources manager.
     */
    @Autowired
    private DataSourcesManager datasourceManager;

    /** The csvim processor. */
    @Autowired
    private CsvimProcessor csvimProcessor;

    /**
     * Import csv.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param table the table
     * @param is the is
     * @throws Exception the exception
     */
    public void importData(String datasource, String schema, String table, InputStream is) throws Exception {
        ImportConfig importConfig = new ImportConfigBuilder().build();
        importData(datasource, schema, table, importConfig, is);
    }

    public void importData(String datasource, String schema, String table, ImportConfig importConfig, InputStream is) throws Exception {
        importData(datasource, schema, table, importConfig.isHeader(), importConfig.isUseHeaderNames(), importConfig.getDelimField(),
                importConfig.getDelimEnclosing(), importConfig.getSequence(), importConfig.isDistinguishEmptyFromNull(), is);
    }

    /**
     * Import csv.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param table the table
     * @param header the header
     * @param useHeaderNames the use header names
     * @param delimField the delim field
     * @param delimEnclosing the delim enclosing
     * @param sequence the sequence
     * @param distinguishEmptyFromNull the distinguish empty from null
     * @param is the is
     * @throws Exception the exception
     */
    public void importData(String datasource, String schema, String table, Boolean header, Boolean useHeaderNames, String delimField,
            String delimEnclosing, String sequence, Boolean distinguishEmptyFromNull, InputStream is) throws Exception {

        DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
        try (Connection connection = dataSource.getConnection()) {
            ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
            if (dataSource.getDatabaseSystem()
                          .isMongoDB()) {
                dialect.importData(connection, table, is);
                return;
            }
            CsvFile csvFile = new CsvFile(null, table, schema, "import", header, useHeaderNames, delimField, delimEnclosing, sequence,
                    distinguishEmptyFromNull, null);
            csvimProcessor.process(csvFile, is, datasource);
        }
    }

    /**
     * Process SQL.
     *
     * @param datasource the datasource
     * @param schema the schema
     * @param is the is
     */
    public void processSQL(String datasource, String schema, InputStream is, long fileSize) throws Exception {
        DirigibleDataSource dataSource = datasourceManager.getDataSource(datasource);
        ISqlDialect dialect = SqlDialectFactory.getDialect(dataSource);
        dialect.processSQL(dataSource, schema, is, fileSize);
    }

}
