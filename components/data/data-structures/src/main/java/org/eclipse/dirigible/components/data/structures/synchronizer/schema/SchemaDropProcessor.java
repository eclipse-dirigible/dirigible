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
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableDropProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableForeignKeysDropProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewDropProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The View Drop Processor.
 */
public class SchemaDropProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SchemaDropProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param schemaModel the schema model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Schema schemaModel) throws SQLException {
        for (View viewModel : schemaModel.getViews()) {
            try {
                ViewDropProcessor.execute(connection, viewModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        for (Table tableModel : schemaModel.getTables()) {
            try {
                TableForeignKeysDropProcessor.execute(connection, tableModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        for (Table tableModel : schemaModel.getTables()) {
            try {
                TableDropProcessor.execute(connection, tableModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
