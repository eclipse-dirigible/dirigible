/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.processors.entity;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.persistence.GenerationType;

import org.eclipse.dirigible.database.persistence.IEntityManagerInterceptor;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableColumnModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.Serializer;
import org.eclipse.dirigible.database.persistence.processors.AbstractPersistenceProcessor;
import org.eclipse.dirigible.database.persistence.processors.identity.PersistenceNextValueIdentityProcessor;
import org.eclipse.dirigible.database.persistence.processors.sequence.PersistenceNextValueSequenceProcessor;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.records.InsertBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Persistence Insert Processor.
 *
 * @param <T> the entity type
 */
public class PersistenceInsertProcessor<T> extends AbstractPersistenceProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceInsertProcessor.class);

    /**
     * Instantiates a new persistence insert processor.
     *
     * @param entityManagerInterceptor the entity manager interceptor
     */
    public PersistenceInsertProcessor(IEntityManagerInterceptor entityManagerInterceptor) {
        super(entityManagerInterceptor);
    }

    /**
     * Generate script.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the string
     */
    @Override
    protected String generateScript(Connection connection, PersistenceTableModel tableModel) {
        InsertBuilder insertBuilder = SqlFactory.getNative(SqlFactory.deriveDialect(connection))
                                                .insert()
                                                .into(tableModel.getTableName());
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            if (columnModel.isIdentity()) {
                continue;
            }
            insertBuilder.column(columnModel.getName());
        }
        String sql = insertBuilder.build();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }
        return sql;
    }

    /**
     * Insert.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @param pojo the pojo
     * @return the identifier of the inserted pojo
     * @throws PersistenceException the persistence exception
     */
    public Object insert(Connection connection, PersistenceTableModel tableModel, T pojo) throws PersistenceException {
        if (logger.isTraceEnabled()) {
            logger.trace("insert -> connection: " + connection.hashCode() + ", tableModel: " + Serializer.serializeTableModel(tableModel)
                    + ", pojo: " + Serializer.serializePojo(pojo));
        }
        Object result = 0;
        String sql = null;
        PreparedStatement preparedStatement = null;
        try {
            boolean identified = setGeneratedValues(connection, tableModel, pojo);
            if (identified) {
                sql = generateScript(connection, tableModel);
                preparedStatement = openPreparedStatement(connection, sql);
                setValuesFromPojo(tableModel, pojo, preparedStatement);
                preparedStatement.executeUpdate();
                result = getPrimaryKeyValue(tableModel, pojo);
            } else {
                sql = generateScript(connection, tableModel);
                preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                setValuesFromPojo(tableModel, pojo, preparedStatement);
                int affectedRows = preparedStatement.executeUpdate();

                if (affectedRows == 0) {
                    // TODO
                    throw new SQLException("No rows affected.");
                }

                try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        result = generatedKeys.getLong(1);
                        for (PersistenceTableColumnModel column : tableModel.getColumns()) {
                            if (column.isPrimaryKey() && column.isIdentity()) {
                                setValueToPojo(pojo, result, column);
                                break;
                            }
                        }
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(sql);
            }
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new PersistenceException(sql, e);
        } finally {
            closePreparedStatement(preparedStatement);
        }
        return result;
    }

    /**
     * Sets the generated values.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @param pojo the pojo
     * @return true, if successful
     * @throws NoSuchFieldException the no such field exception
     * @throws IllegalAccessException the illegal access exception
     * @throws SQLException the SQL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private boolean setGeneratedValues(Connection connection, PersistenceTableModel tableModel, Object pojo)
            throws NoSuchFieldException, IllegalAccessException, SQLException, IOException {
        if (logger.isTraceEnabled()) {
            logger.trace("setGeneratedValues -> connection: " + connection.hashCode() + ", tableModel: "
                    + Serializer.serializeTableModel(tableModel) + ", pojo: " + Serializer.serializePojo(pojo));
        }
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            if (columnModel.isPrimaryKey() && (columnModel.getGenerated() != null)) {
                long id = -1;
                if (GenerationType.SEQUENCE.name()
                                           .equals(columnModel.getGenerated())) {
                    PersistenceNextValueSequenceProcessor persistenceNextValueSequenceProcessor =
                            new PersistenceNextValueSequenceProcessor(getEntityManagerInterceptor());
                    id = persistenceNextValueSequenceProcessor.nextval(connection, tableModel);
                } else if (GenerationType.TABLE.name()
                                               .equals(columnModel.getGenerated())) {
                    PersistenceNextValueIdentityProcessor persistenceNextValueIdentityProcessor =
                            new PersistenceNextValueIdentityProcessor(getEntityManagerInterceptor());
                    id = persistenceNextValueIdentityProcessor.nextval(connection, tableModel);
                } else if (GenerationType.IDENTITY.name()
                                                  .equals(columnModel.getGenerated())) {
                    return false;
                } else {
                    throw new IllegalArgumentException(format("Generation Type: [{0}] not supported.", columnModel.getGenerated()));
                }
                setValueToPojo(pojo, id, columnModel);
            }
        }
        return true;
    }

    /**
     * Gets the primary key value.
     *
     * @param tableModel the table model
     * @param pojo the pojo
     * @return the primary key value
     * @throws NoSuchFieldException the no such field exception
     * @throws IllegalAccessException the illegal access exception
     * @throws SQLException the SQL exception
     */
    private Object getPrimaryKeyValue(PersistenceTableModel tableModel, Object pojo)
            throws NoSuchFieldException, IllegalAccessException, SQLException {
        for (PersistenceTableColumnModel columnModel : tableModel.getColumns()) {
            if (columnModel.isPrimaryKey()) {
                return getValueFromPojo(pojo, columnModel);
            }
        }
        return null;
    }

}
