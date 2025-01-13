/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintForeignKey;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.text.MessageFormat.format;

/**
 * The Table Drop Processor.
 */
public class TableDropProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TableDropProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel) throws SQLException {
        String tableName = "\"" + tableModel.getName() + "\"";

        if (logger.isInfoEnabled()) {
            logger.info("Processing Drop Table: " + tableName);
        }
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, tableName)) {
            String sql = SqlFactory.getNative(connection)
                                   .select()
                                   .column("COUNT(*)")
                                   .from(tableName)
                                   .build();
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                if (logger.isInfoEnabled()) {
                    logger.info(sql);
                }
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    if (count > 0) {
                        if (logger.isErrorEnabled()) {
                            logger.error(format(
                                    "Drop operation for the non empty Table [{0}] will not be executed. Delete all the records in the table first.",
                                    tableName));
                        }
                        return;
                    }
                }
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

            if (tableModel.getConstraints()
                          .getForeignKeys() != null
                    && !tableModel.getConstraints()
                                  .getForeignKeys()
                                  .isEmpty()) {
                for (TableConstraintForeignKey foreignKeyModel : tableModel.getConstraints()
                                                                           .getForeignKeys()) {
                    sql = SqlFactory.getNative(connection)
                                    .drop()
                                    .constraint(foreignKeyModel.getName())
                                    .fromTable(tableName)
                                    .build();
                    executeUpdate(connection, sql);
                }
            }

            sql = SqlFactory.getNative(connection)
                            .drop()
                            .table(tableName)
                            .build();
            executeUpdate(connection, sql);
        }
    }

    /**
     * Execute update.
     *
     * @param connection the connection
     * @param sql the sql
     * @throws SQLException the SQL exception
     */
    private static void executeUpdate(Connection connection, String sql) throws SQLException {
        PreparedStatement statement;
        if (logger.isInfoEnabled()) {
            logger.info(sql);
        }
        statement = connection.prepareStatement(sql);
        try {
            statement.executeUpdate();
        } catch (SQLException e) {
            if (logger.isWarnEnabled()) {
                logger.warn(sql);
            }
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage());
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

}
