/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableForeignKeysDropProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewDropProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Schema Update Processor.
 */
public class SchemaUpdateProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemaUpdateProcessor.class);

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
                    TableAlterProcessor.execute(connection, tableModel);
                }
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        for (Table tableModel : schemaModel.getTables()) {
            try {
                TableForeignKeysDropProcessor.execute(connection, tableModel);
                TableForeignKeysCreateProcessor.execute(connection, tableModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        for (View viewModel : schemaModel.getViews()) {
            try {
                ViewDropProcessor.execute(connection, viewModel);
                ViewCreateProcessor.execute(connection, viewModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

    }

}
