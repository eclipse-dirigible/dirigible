/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.utils;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.dirigible.components.api.platform.ProblemsFacade;
import org.eclipse.dirigible.components.data.csvim.processor.CsvProcessor;
import org.eclipse.dirigible.components.data.csvim.processor.CsvimProcessor;
import org.eclipse.dirigible.components.database.domain.TableMetadata;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CsvimUtils.
 */
public class CsvimUtils {

    /** The Constant PROGRAM_DEFAULT. */
    public static final String PROGRAM_DEFAULT = "Eclipse Dirigible";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CsvProcessor.class);

    /**
     * Use to log errors from artifact processing.
     *
     * @param errorMessage the error message
     * @param errorType the error type
     * @param location the location
     * @param artifactType the artifact type
     * @param module the module
     */
    public static void logProcessorErrors(String errorMessage, String errorType, String location, String artifactType, String module) {
        try {
            ProblemsFacade.save(location, errorType, "", "", errorMessage, "", artifactType, module, CsvimProcessor.class.getName(),
                    PROGRAM_DEFAULT);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("There is an issue with logging of the Errors.");
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Gets the table metadata.
     *
     * @param tableName the table name
     * @param schema the schema
     * @param connection the connection
     * @return the table metadata
     */
    public static TableMetadata getTableMetadata(String tableName, String schema, Connection connection) {
        try {
            TableMetadata metadata = DatabaseMetadataHelper.describeTable(connection, null, schema, tableName);
            return metadata != null ? metadata
                    : DatabaseMetadataHelper.describeTable(connection, null, DatabaseMetadataHelper.getTableSchema(connection, tableName),
                            tableName);
        } catch (SQLException sqlException) {
            logger.error("Error occurred while trying to read metadata for table [{}]. Returning null.", tableName, sqlException);
            return null;
        }
    }
}
