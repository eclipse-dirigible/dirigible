/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.view;

import org.eclipse.dirigible.components.data.structures.domain.View;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The View Create Processor.
 */
public class ViewCreateProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ViewCreateProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param viewModel the view model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, View viewModel) throws SQLException {
        String viewName = "\"" + viewModel.getName() + "\"";

        logger.info("Processing Create View: " + viewName);

        if (!SqlFactory.getNative(connection)
                       .existsTable(connection, viewName)) {
            String sql = SqlFactory.getNative(connection)
                                   .create()
                                   .view(viewName)
                                   .asSelect(viewModel.getQuery())
                                   .build();
            logger.info("About to execute sql [{}]", sql);
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                statement.executeUpdate();
            } catch (SQLException ex) {
                String errorMessage = "Failed to create view [" + viewModel + "] using sql [" + sql + "]";
                logger.error(errorMessage, viewModel, sql, ex);
                throw new SQLException(errorMessage, ex);
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    }

}
