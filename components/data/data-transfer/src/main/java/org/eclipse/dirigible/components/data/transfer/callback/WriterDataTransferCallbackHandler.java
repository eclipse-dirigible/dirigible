/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.callback;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.eclipse.dirigible.components.data.transfer.domain.DataTransferConfiguration;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class WriterDataTransferCallbackHandler.
 */
public class WriterDataTransferCallbackHandler implements DataTransferCallbackHandler {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(WriterDataTransferCallbackHandler.class);

    /** The Constant SEVERITY_INFO. */
    private static final String SEVERITY_INFO = "INFO";

    /** The Constant SEVERITY_ERROR. */
    private static final String SEVERITY_ERROR = "ERROR";

    /** The Constant SEVERITY_WARNING. */
    private static final String SEVERITY_WARNING = "WARNING";


    /** The writer. */
    private Writer writer;

    /** The identifier. */
    private String identifier;

    /** The stopped. */
    private boolean stopped = false;

    /**
     * Instantiates a new writer data transfer callback handler.
     *
     * @param writer the writer
     * @param identifier the identifier
     */
    public WriterDataTransferCallbackHandler(Writer writer, String identifier) {
        this.writer = writer;
        this.identifier = identifier;
    }

    /**
     * Write.
     *
     * @param s the s
     * @param severity the severity
     */
    private void write(String s, String severity) {
        try {
            String message = String.format("[%s][%s] %s", identifier, severity, s);
            this.writer.write(message);
            this.writer.write("\n");
            this.writer.flush();
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Gets the identifier.
     *
     * @return the identifier
     */
    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Sets the identifier.
     *
     * @param identifier the new identifier
     */
    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Transfer started.
     *
     * @param configuration the configuration
     */
    @Override
    public void transferStarted(DataTransferConfiguration configuration) {
        write("Transfer has been started...", SEVERITY_INFO);
    }

    /**
     * Transfer finished.
     *
     * @param count the count
     */
    @Override
    public void transferFinished(int count) {
        write("Transfer has been finished successfully for tables count: " + count, SEVERITY_INFO);
        try {
            this.writer.close();
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Transfer failed.
     *
     * @param error the error
     */
    @Override
    public void transferFailed(String error) {
        write("Transfer failed with error: " + error, SEVERITY_ERROR);
    }

    /**
     * Metadata loading started.
     */
    @Override
    public void metadataLoadingStarted() {
        write("Loading of metadata has been started...", SEVERITY_INFO);
    }

    /**
     * Metadata loading error.
     *
     * @param error the error
     */
    @Override
    public void metadataLoadingError(String error) {
        write(error, SEVERITY_ERROR);

    }

    /**
     * Metadata loading finished.
     *
     * @param count the count
     */
    @Override
    public void metadataLoadingFinished(int count) {
        write("Loading of metadata has been finished successfully - tables count is: " + count, SEVERITY_INFO);
    }

    /**
     * Sorting started.
     *
     * @param tables the tables
     */
    @Override
    public void sortingStarted(List<PersistenceTableModel> tables) {
        write("Topological sorting of tables via dependencies has been started...", SEVERITY_INFO);
    }

    /**
     * Sorting finished.
     *
     * @param result the result
     */
    @Override
    public void sortingFinished(List<PersistenceTableModel> result) {
        StringBuffer buffer = new StringBuffer();
        for (PersistenceTableModel model : result) {
            buffer.append(model.getTableName() + ", ");
        }
        write("Loading of metadata has been finished successfully - tables count is: " + buffer.substring(0, buffer.length() - 2),
                SEVERITY_INFO);
    }

    /**
     * Data transfer started.
     */
    @Override
    public void dataTransferStarted() {
        write("Data transfer has been started...", SEVERITY_INFO);

    }

    /**
     * Data transfer finished.
     */
    @Override
    public void dataTransferFinished() {
        write("Data transfer has been finished successfully.", SEVERITY_INFO);

    }

    /**
     * Table transfer started.
     *
     * @param table the table
     */
    @Override
    public void tableTransferStarted(String table) {
        write("Data transfer has been started for table: " + table, SEVERITY_INFO);

    }

    /**
     * Table transfer finished.
     *
     * @param table the table
     * @param transferedRecords the transfered records
     */
    @Override
    public void tableTransferFinished(String table, int transferedRecords) {
        write("Data transfer has been finished successfully for table: " + table + " with records count: " + transferedRecords,
                SEVERITY_INFO);

    }

    /**
     * Table transfer failed.
     *
     * @param table the table
     * @param error the error
     */
    @Override
    public void tableTransferFailed(String table, String error) {
        write("Data transfer has been failed for table: " + table + " with error: " + error, SEVERITY_ERROR);
    }

    /**
     * Record transfer finished.
     *
     * @param tableName the table name
     * @param i the i
     */
    @Override
    public void recordTransferFinished(String tableName, int i) {
        //
    }

    /**
     * Table select SQL.
     *
     * @param selectSQL the select SQL
     */
    @Override
    public void tableSelectSQL(String selectSQL) {
        if (logger.isDebugEnabled()) {
            logger.debug("Table select SQL script is: " + selectSQL, SEVERITY_INFO);
        }

    }

    /**
     * Table insert SQL.
     *
     * @param insertSQL the insert SQL
     */
    @Override
    public void tableInsertSQL(String insertSQL) {
        if (logger.isDebugEnabled()) {
            logger.debug("Table select SQL script is: " + insertSQL, SEVERITY_INFO);
        }
    }

    /**
     * Table skipped.
     *
     * @param table the table
     * @param reason the reason
     */
    @Override
    public void tableSkipped(String table, String reason) {
        if (logger.isDebugEnabled()) {
            logger.debug("Table " + table + " has been skipped due to: " + reason, SEVERITY_WARNING);
        }

    }

    /**
     * Stop transfer.
     */
    @Override
    public void stopTransfer() {
        stopped = true;
        if (logger.isDebugEnabled()) {
            logger.debug("Transfer has been stopped.", SEVERITY_WARNING);
        }
    }

    /**
     * Checks if is stopped.
     *
     * @return true, if is stopped
     */
    @Override
    public boolean isStopped() {
        return stopped;
    }


}
