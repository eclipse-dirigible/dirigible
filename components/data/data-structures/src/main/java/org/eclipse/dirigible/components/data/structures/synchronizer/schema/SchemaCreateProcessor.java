/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.schema;

import java.sql.SQLException;

import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableAlterProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableForeignKeysCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewCreateProcessor;
import org.eclipse.dirigible.components.data.structures.synchronizer.view.ViewDropProcessor;
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
     * Execute the corresponding statement. A refused table is logged and skipped, the next one starts
     * on a usable connection, and the refused tables are reported once every other table, foreign key
     * and view had its turn (#7766).
     *
     * @param connection the connection of the schema pass
     * @param schemaModel the schema model
     * @throws SQLException naming the refused tables, or when no connection can be acquired
     */
    public static void execute(SchemaPassConnection connection, Schema schemaModel) throws SQLException {
        RefusedTables refusedTables = new RefusedTables();

        for (Table tableModel : schemaModel.getTables()) {
            try {
                if (!SqlFactory.getNative(connection.get())
                               .existsTable(connection.get(), tableModel.getName())) {
                    TableCreateProcessor.execute(connection.get(), tableModel, true);
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("Table [%s] already exists during the create process, hence will be altered.",
                                tableModel.getName()));
                    }
                    TableAlterProcessor.execute(connection.get(), tableModel);
                }
            } catch (SQLException | RuntimeException ex) {
                logger.error("Failed to create or alter table [{}] of schema [{}]", tableModel.getName(), schemaModel.getName(), ex);
                refusedTables.add(tableModel.getName(), ex);
                connection.recoverAfterRefusal();
            }
        }

        for (Table tableModel : schemaModel.getTables()) {
            try {
                TableForeignKeysCreateProcessor.execute(connection.get(), tableModel);
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                connection.recoverAfterRefusal();
            }
        }

        for (View viewModel : schemaModel.getViews()) {
            try {
                if (!SqlFactory.getNative(connection.get())
                               .existsTable(connection.get(), viewModel.getName())) {
                    try {
                        ViewCreateProcessor.execute(connection.get(), viewModel);
                    } catch (Exception e) {
                        if (logger.isErrorEnabled()) {
                            logger.error(e.getMessage(), e);
                        }
                        connection.recoverAfterRefusal();
                    }
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("View [%s] already exists during the create process, hence will be recreated.",
                                viewModel.getName()));
                    }
                    ViewDropProcessor.execute(connection.get(), viewModel);
                    ViewCreateProcessor.execute(connection.get(), viewModel);
                }
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
                connection.recoverAfterRefusal();
            }
        }

        refusedTables.throwIfAny(schemaModel);
    }

}
