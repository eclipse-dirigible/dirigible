/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.schema;

import java.sql.Connection;
import java.sql.SQLException;

import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableAlterProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableForeignKeysCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewDropProcessor;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Schema Create Processor.
 */
public class SchemaCreateProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemaCreateProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param schemaModel the schema model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Schema schemaModel) throws SQLException {

        for (Table tableModel : schemaModel.getTables()) {
            try {
                if (!SqlFactory.getNative(connection)
                               .existsTable(connection, tableModel.getName())) {
                    try {
                        TableCreateProcessor.execute(connection, tableModel, true);
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("Table [%s] already exists during the create process, hence will be altered.",
                                tableModel.getName()));
                    }
                    TableAlterProcessor.execute(connection, tableModel);
                }
            } catch (SQLException | SqlException ex) {
                logger.error("Failed to alter table [{}]", tableModel, ex);
            }
        }

        for (Table tableModel : schemaModel.getTables()) {
            try {
                TableForeignKeysCreateProcessor.execute(connection, tableModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        for (View viewModel : schemaModel.getViews()) {
            try {
                if (!SqlFactory.getNative(connection)
                               .existsTable(connection, viewModel.getName())) {
                    try {
                        ViewCreateProcessor.execute(connection, viewModel);
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("View [%s] already exists during the create process, hence will be recreated.",
                                viewModel.getName()));
                    }
                    ViewDropProcessor.execute(connection, viewModel);
                    ViewCreateProcessor.execute(connection, viewModel);
                }
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

}
