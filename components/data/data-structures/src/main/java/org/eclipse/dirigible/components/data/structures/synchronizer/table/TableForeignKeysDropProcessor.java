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
import org.eclipse.dirigible.database.sql.builders.table.AlterTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The Class TableForeignKeysDropProcessor.
 */
public class TableForeignKeysDropProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TableForeignKeysDropProcessor.class);

    /**
     * Execute the corresponding statement.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public static void execute(Connection connection, Table tableModel) throws SQLException {
        String tableName = "\"" + tableModel.getName() + "\"";

        if (tableModel.getConstraints() != null) {
            if (tableModel.getConstraints()
                          .getForeignKeys() != null
                    && !tableModel.getConstraints()
                                  .getForeignKeys()
                                  .isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Processing Alter Table Create Foreign Keys Table: " + tableName);
                }
                AlterTableBuilder alterTableBuilder = SqlFactory.getNative(connection)
                                                                .alter()
                                                                .table(tableName);
                for (TableConstraintForeignKey foreignKey : tableModel.getConstraints()
                                                                      .getForeignKeys()) {
                    String foreignKeyName = "\"" + foreignKey.getName() + "\"";

                    alterTableBuilder.drop()
                                     .foreignKey(foreignKeyName, foreignKey.getColumns(), foreignKey.getReferencedTable(),
                                             foreignKey.getReferencedColumns());
                }

                final String sql = alterTableBuilder.build();
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
                    throw new SQLException(e.getMessage(), e);
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                }
            }
        }
    }

}
