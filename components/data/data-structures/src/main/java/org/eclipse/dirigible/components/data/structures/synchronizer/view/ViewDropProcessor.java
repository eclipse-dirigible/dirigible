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
 * The View Drop Processor.
 */
public class ViewDropProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ViewDropProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param viewModel the view model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, View viewModel) throws SQLException {
        String viewName = "\"" + viewModel.getName() + "\"";

        if (logger.isInfoEnabled()) {
            logger.info("Processing Drop View: " + viewName);
        }
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, viewName)) {
            String sql = SqlFactory.getNative(connection)
                                   .drop()
                                   .view(viewName)
                                   .build();
            if (logger.isInfoEnabled()) {
                logger.info(sql);
            }
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                statement.executeUpdate();
            } catch (SQLException e) {
                if (logger.isErrorEnabled()) {
                    logger.error(sql);
                }
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            } finally {
                if (statement != null) {
                    statement.close();
                }
            }
        }
    }

}
