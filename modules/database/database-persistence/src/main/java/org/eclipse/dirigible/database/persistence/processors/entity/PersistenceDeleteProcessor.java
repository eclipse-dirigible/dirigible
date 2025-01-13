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

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.eclipse.dirigible.database.persistence.IEntityManagerInterceptor;
import org.eclipse.dirigible.database.persistence.PersistenceException;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.parser.Serializer;
import org.eclipse.dirigible.database.persistence.processors.AbstractPersistenceProcessor;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.builders.records.DeleteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Persistence Delete Processor.
 *
 * @param <T> the generic type
 */
public class PersistenceDeleteProcessor<T> extends AbstractPersistenceProcessor {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PersistenceDeleteProcessor.class);

    /**
     * Instantiates a new persistence delete processor.
     *
     * @param entityManagerInterceptor the entity manager interceptor
     */
    public PersistenceDeleteProcessor(IEntityManagerInterceptor entityManagerInterceptor) {
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
        throw new PersistenceException("Generate Script method cannot be invoked in Delete Processor");
    }

    /**
     * Generate script delete.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the string
     */
    protected String generateScriptDelete(Connection connection, PersistenceTableModel tableModel) {
        DeleteBuilder deleteBuilder = SqlFactory.getNative(SqlFactory.deriveDialect(connection))
                                                .delete()
                                                .from(tableModel.getTableName())
                                                .where(getPrimaryKey(tableModel) + new StringBuilder().append(ISqlKeywords.SPACE)
                                                                                                      .append(ISqlKeywords.EQUALS)
                                                                                                      .append(ISqlKeywords.SPACE)
                                                                                                      .append(ISqlKeywords.QUESTION)
                                                                                                      .toString());
        String sql = deleteBuilder.toString();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }
        return sql;
    }

    /**
     * Generate script delete all.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @return the string
     */
    protected String generateScriptDeleteAll(Connection connection, PersistenceTableModel tableModel) {
        DeleteBuilder deleteBuilder = SqlFactory.getNative(SqlFactory.deriveDialect(connection))
                                                .delete()
                                                .from(tableModel.getTableName());
        String sql = deleteBuilder.toString();
        if (logger.isTraceEnabled()) {
            logger.trace(sql);
        }
        return sql;
    }

    /**
     * Delete.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @param clazz the clazz
     * @param id the id
     * @return the int
     * @throws PersistenceException the persistence exception
     */
    public int delete(Connection connection, PersistenceTableModel tableModel, Class<T> clazz, Object id) throws PersistenceException {
        if (logger.isTraceEnabled()) {
            logger.trace("delete -> connection: " + connection.hashCode() + ", tableModel: " + Serializer.serializeTableModel(tableModel)
                    + ", class: " + clazz.getCanonicalName() + ", id: " + id);
        }
        String sql = null;
        PreparedStatement preparedStatement = null;
        try {
            sql = generateScriptDelete(connection, tableModel);
            preparedStatement = openPreparedStatement(connection, sql);
            setValue(preparedStatement, 1, id);
            return preparedStatement.executeUpdate();
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
    }

    /**
     * Delete all.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @param clazz the clazz
     * @return the int
     * @throws PersistenceException the persistence exception
     */
    public int deleteAll(Connection connection, PersistenceTableModel tableModel, Class<T> clazz) throws PersistenceException {
        if (logger.isTraceEnabled()) {
            logger.trace("deleteAll -> connection: " + connection.hashCode() + ", tableModel: " + Serializer.serializeTableModel(tableModel)
                    + ", class: " + clazz.getCanonicalName());
        }
        String sql = null;
        PreparedStatement preparedStatement = null;
        try {
            sql = generateScriptDeleteAll(connection, tableModel);
            preparedStatement = openPreparedStatement(connection, sql);
            return preparedStatement.executeUpdate();
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
    }

}
