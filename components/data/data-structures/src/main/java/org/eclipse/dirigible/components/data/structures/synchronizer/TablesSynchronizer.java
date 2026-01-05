/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.*;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.*;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

/**
 * The Class TablesSynchronizer.
 */
@Component
@Order(SynchronizersOrder.TABLE)
public class TablesSynchronizer extends MultitenantBaseSynchronizer<Table, Long> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TablesSynchronizer.class);

    /** The Constant FILE_EXTENSION_TABLE. */
    private static final String FILE_EXTENSION_TABLE = ".table";

    /** The table service. */
    private final TableService tableService;

    /** The datasources manager. */
    private final DataSourcesManager datasourcesManager;

    /** The synchronization callback. */
    private SynchronizerCallback callback;

    /**
     * Instantiates a new table synchronizer.
     *
     * @param tableService the table service
     * @param datasourcesManager the datasources manager
     */
    @Autowired
    public TablesSynchronizer(TableService tableService, DataSourcesManager datasourcesManager) {
        this.tableService = tableService;
        this.datasourcesManager = datasourcesManager;
    }

    /**
     * Checks if is accepted.
     *
     * @param type the type
     * @return true, if is accepted
     */
    @Override
    public boolean isAccepted(String type) {
        return Table.ARTEFACT_TYPE.equals(type);
    }

    /**
     * Load.
     *
     * @param location the location
     * @param content the content
     * @return the list
     * @throws ParseException the parse exception
     */
    @Override
    protected List<Table> parseImpl(String location, byte[] content) throws ParseException {
        Table table = JsonHelper.fromJson(new String(content, StandardCharsets.UTF_8), Table.class);
        Configuration.configureObject(table);
        table.setLocation(location);
        if (table.getKind() == null) {
            table.setKind(table.getType());
        }
        table.setType(Table.ARTEFACT_TYPE);
        table.updateKey();
        assignParent(table);

        try {
            Table maybe = getService().findByKey(table.getKey());
            if (maybe != null) {
                table.setId(maybe.getId());
                reassignIds(table, maybe);
            }
            Table result = getService().save(table);
            return List.of(result);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            if (logger.isErrorEnabled()) {
                logger.error("table: {}", table);
            }
            if (logger.isErrorEnabled()) {
                logger.error("content: {}", new String(content));
            }
            throw new ParseException(e.getMessage(), 0);
        }
    }

    /**
     * Assign parent.
     *
     * @param table the table
     */
    static void assignParent(Table table) {
        table.getColumns()
             .forEach(c -> c.setTable(table));
        if (table.getIndexes() != null) {
            table.getIndexes()
                 .forEach(i -> i.setTable(table));
        }
        if (table.getConstraints() != null) {
            table.getConstraints()
                 .setTable(table);
            if (table.getConstraints()
                     .getPrimaryKey() != null) {
                table.getConstraints()
                     .getPrimaryKey()
                     .setConstraints(table.getConstraints());
            }
            if (table.getConstraints()
                     .getForeignKeys() != null) {
                table.getConstraints()
                     .getForeignKeys()
                     .forEach(fk -> fk.setConstraints(table.getConstraints()));
            }
            if (table.getConstraints()
                     .getUniqueIndexes() != null) {
                table.getConstraints()
                     .getUniqueIndexes()
                     .forEach(u -> u.setConstraints(table.getConstraints()));
            }
            if (table.getConstraints()
                     .getChecks() != null) {
                table.getConstraints()
                     .getChecks()
                     .forEach(c -> c.setConstraints(table.getConstraints()));
            }
        }
    }

    /**
     * Gets the service.
     *
     * @return the service
     */
    @Override
    public ArtefactService<Table, Long> getService() {
        return tableService;
    }

    /**
     * Reassign ids.
     *
     * @param table the table
     * @param maybe the maybe
     */
    static void reassignIds(Table table, Table maybe) {
        table.getColumns()
             .forEach(c -> {
                 TableColumn m = maybe.getColumn(c.getName());
                 if (m != null) {
                     c.setId(m.getId());
                 }
             });
        if (table.getIndexes() != null) {
            table.getIndexes()
                 .forEach(i -> {
                     TableIndex m = maybe.getIndex(i.getName());
                     if (m != null) {
                         i.setId(m.getId());
                     }
                 });
        }
        if (table.getConstraints() != null) {
            table.getConstraints()
                 .setId(maybe.getConstraints()
                             .getId());
            if (table.getConstraints()
                     .getPrimaryKey() != null
                    && maybe.getConstraints()
                            .getPrimaryKey() != null) {
                table.getConstraints()
                     .getPrimaryKey()
                     .setId(maybe.getConstraints()
                                 .getPrimaryKey()
                                 .getId());
            }
            if (table.getConstraints()
                     .getForeignKeys() != null
                    && maybe.getConstraints()
                            .getForeignKeys() != null) {
                table.getConstraints()
                     .getForeignKeys()
                     .forEach(fk -> {
                         TableConstraintForeignKey m = maybe.getConstraints()
                                                            .getForeignKey(fk.getName());
                         if (m != null) {
                             fk.setId(m.getId());
                         }
                     });
            }
            if (table.getConstraints()
                     .getUniqueIndexes() != null
                    && maybe.getConstraints()
                            .getUniqueIndexes() != null) {
                table.getConstraints()
                     .getUniqueIndexes()
                     .forEach(ui -> {
                         TableConstraintUnique m = maybe.getConstraints()
                                                        .getUniqueIndex(ui.getName());
                         if (m != null) {
                             ui.setId(m.getId());
                         }
                     });
            }
            if (table.getConstraints()
                     .getChecks() != null
                    && maybe.getConstraints()
                            .getChecks() != null) {
                table.getConstraints()
                     .getChecks()
                     .forEach(ck -> {
                         TableConstraintCheck m = maybe.getConstraints()
                                                       .getCheck(ck.getName());
                         if (m != null) {
                             ck.setId(m.getId());
                         }
                     });
            }
        }
    }

    /**
     * Retrieve.
     *
     * @param location the location
     * @return the list
     */
    @Override
    public List<Table> retrieve(String location) {
        return getService().findByLocation(location);
    }

    /**
     * Sets the status.
     *
     * @param artefact the artefact
     * @param lifecycle the lifecycle
     * @param error the error
     */
    @Override
    public void setStatus(Table artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    /**
     * Complete impl.
     *
     * @param wrapper the wrapper
     * @param flow the flow
     * @return true, if successful
     */
    @Override
    protected boolean completeImpl(TopologyWrapper<Table> wrapper, ArtefactPhase flow) {
        Table table = wrapper.getArtefact();

        try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            switch (flow) {
                case CREATE:
                    if (ArtefactLifecycle.NEW.equals(table.getLifecycle())) {
                        if (!SqlFactory.getNative(connection)
                                       .existsTable(connection, table.getName())) {
                            try {
                                executeTableCreate(connection, table);
                            } catch (Exception e) {
                                if (logger.isErrorEnabled()) {
                                    logger.error(e.getMessage(), e);
                                }
                                callback.registerState(this, wrapper, ArtefactLifecycle.CREATED, e);
                            }
                        } else {
                            if (logger.isWarnEnabled()) {
                                logger.warn(String.format("Table [%s] already exists during the update process", table.getName()));
                            }
                            executeTableAlter(connection, table);
                            callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                        }
                        callback.registerState(this, wrapper, ArtefactLifecycle.CREATED);
                    }
                    break;
                case UPDATE:
                    if (ArtefactLifecycle.CREATED.equals(table.getLifecycle())) {
                        if (SqlFactory.getNative(connection)
                                      .existsTable(connection, table.getName())) {
                            try {
                                executeTableForeignKeysCreate(connection, table);
                            } catch (SQLException e) {
                                if (logger.isWarnEnabled()) {
                                    logger.warn(e.getMessage());
                                }
                            }
                        }
                    }
                    if (ArtefactLifecycle.MODIFIED.equals(table.getLifecycle())) {
                        executeTableUpdate(connection, table);
                        callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                    }
                    if (ArtefactLifecycle.FAILED.equals(table.getLifecycle())) {
                        return false;
                    }
                    break;
                case DELETE:
                    if (ArtefactLifecycle.CREATED.equals(table.getLifecycle()) || ArtefactLifecycle.UPDATED.equals(table.getLifecycle())
                            || ArtefactLifecycle.FAILED.equals(table.getLifecycle())) {
                        if (SqlFactory.getNative(connection)
                                      .existsTable(connection, table.getName())) {
                            if (SqlFactory.deriveDialect(connection)
                                          .count(connection, table.getName()) == 0) {
                                executeTableDrop(connection, table);
                                callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                            } else {
                                String message = String.format(
                                        "Table [%s] cannot be deleted during the update process, because it is not empty", table.getName());
                                if (logger.isWarnEnabled()) {
                                    logger.warn(message);
                                }
                                callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, message);
                            }
                        }
                    }
                    break;
                // case POST_DELETE:
                // if (table.getLifecycle().equals(ArtefactLifecycle.DELETED)) {
                // if (SqlFactory.getNative(connection).exists(connection, table.getName())) {
                // executeTableForeignKeysDrop(connection, table);
                // callback.registerState(this, wrapper, ArtefactLifecycle.UPDATED);
                // }
                // }
                case START:
                case STOP:
            }

            return true;
        } catch (SQLException e) {
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    /**
     * Execute table create.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableCreate(Connection connection, Table tableModel) throws SQLException {
        TableCreateProcessor.execute(connection, tableModel, true);
    }

    /**
     * Execute table alter.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableAlter(Connection connection, Table tableModel) throws SQLException {
        TableAlterProcessor.execute(connection, tableModel);
    }

    /**
     * Execute table foreign keys create.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableForeignKeysCreate(Connection connection, Table tableModel) throws SQLException {
        TableForeignKeysCreateProcessor.execute(connection, tableModel);
    }

    /**
     * Execute table update.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableUpdate(Connection connection, Table tableModel) throws SQLException {
        if (logger.isInfoEnabled()) {
            logger.info("Processing Update Table: " + tableModel.getName());
        }
        if (SqlFactory.getNative(connection)
                      .existsTable(connection, tableModel.getName())) {
            // if (SqlFactory.getNative(connection).count(connection, tableModel.getName()) == 0) {
            // executeTableDrop(connection, tableModel);
            // executeTableCreate(connection, tableModel);
            // } else {
            executeTableAlter(connection, tableModel);
            // }
        } else {
            executeTableCreate(connection, tableModel);
        }
    }

    /**
     * Execute table drop.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableDrop(Connection connection, Table tableModel) throws SQLException {
        TableDropProcessor.execute(connection, tableModel);
    }

    /**
     * Cleanup.
     *
     * @param table the table
     */
    @Override
    public void cleanupImpl(Table table) {
        try (Connection connection = datasourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            if (SqlFactory.getNative(connection)
                          .existsTable(connection, table.getName())) {
                if (SqlFactory.deriveDialect(connection)
                              .count(connection, table.getName()) == 0) {
                    executeTableDrop(connection, table);
                    getService().delete(table);
                } else {
                    String message = String.format("Table [%s] cannot be deleted during the update process, because it is not empty",
                            table.getName());
                    if (logger.isWarnEnabled()) {
                        logger.warn(message);
                    }
                }
            }
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, table, ArtefactLifecycle.DELETED, e);
        }
    }

    /**
     * Sets the callback.
     *
     * @param callback the new callback
     */
    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    /**
     * Execute table foreign keys drop.
     *
     * @param connection the connection
     * @param tableModel the table model
     * @throws SQLException the SQL exception
     */
    public void executeTableForeignKeysDrop(Connection connection, Table tableModel) throws SQLException {
        TableForeignKeysDropProcessor.execute(connection, tableModel);
    }

    /**
     * Gets the file extension.
     *
     * @return the file extension
     */
    @Override
    public String getFileExtension() {
        return FILE_EXTENSION_TABLE;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    @Override
    public String getArtefactType() {
        return Table.ARTEFACT_TYPE;
    }

}
