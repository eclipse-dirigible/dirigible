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

import java.util.List;

import org.eclipse.dirigible.components.data.transfer.domain.DataTransferConfiguration;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;

/**
 * The Class DummyDataTransferCallbackHandler.
 */
public class DummyDataTransferCallbackHandler implements DataTransferCallbackHandler {

    /**
     * Gets the identifier.
     *
     * @return the identifier
     */
    @Override
    public String getIdentifier() {
        return null;
    }

    /**
     * Sets the identifier.
     *
     * @param identifier the new identifier
     */
    @Override
    public void setIdentifier(String identifier) {

    }

    /**
     * Transfer started.
     *
     * @param configuration the configuration
     */
    @Override
    public void transferStarted(DataTransferConfiguration configuration) {

    }

    /**
     * Transfer finished.
     *
     * @param count the count
     */
    @Override
    public void transferFinished(int count) {}

    /**
     * Transfer failed.
     *
     * @param error the error
     */
    @Override
    public void transferFailed(String error) {}

    /**
     * Metadata loading started.
     */
    @Override
    public void metadataLoadingStarted() {}

    /**
     * Metadata loading error.
     *
     * @param error the error
     */
    @Override
    public void metadataLoadingError(String error) {}

    /**
     * Metadata loading finished.
     *
     * @param count the count
     */
    @Override
    public void metadataLoadingFinished(int count) {}

    /**
     * Sorting started.
     *
     * @param tables the tables
     */
    @Override
    public void sortingStarted(List<PersistenceTableModel> tables) {}

    /**
     * Sorting finished.
     *
     * @param result the result
     */
    @Override
    public void sortingFinished(List<PersistenceTableModel> result) {}

    /**
     * Data transfer started.
     */
    @Override
    public void dataTransferStarted() {}

    /**
     * Data transfer finished.
     */
    @Override
    public void dataTransferFinished() {}

    /**
     * Table transfer started.
     *
     * @param table the table
     */
    @Override
    public void tableTransferStarted(String table) {}

    /**
     * Table transfer finished.
     *
     * @param table the table
     * @param transferedRecords the transfered records
     */
    @Override
    public void tableTransferFinished(String table, int transferedRecords) {}

    /**
     * Table transfer failed.
     *
     * @param table the table
     * @param error the error
     */
    @Override
    public void tableTransferFailed(String table, String error) {}

    /**
     * Record transfer finished.
     *
     * @param tableName the table name
     * @param i the i
     */
    @Override
    public void recordTransferFinished(String tableName, int i) {}

    /**
     * Table select SQL.
     *
     * @param selectSQL the select SQL
     */
    @Override
    public void tableSelectSQL(String selectSQL) {}

    /**
     * Table insert SQL.
     *
     * @param insertSQL the insert SQL
     */
    @Override
    public void tableInsertSQL(String insertSQL) {}

    /**
     * Table skipped.
     *
     * @param table the table
     * @param reason the reason
     */
    @Override
    public void tableSkipped(String table, String reason) {}

    /**
     * Stop transfer.
     */
    @Override
    public void stopTransfer() {}

    /**
     * Checks if is stopped.
     *
     * @return true, if is stopped
     */
    @Override
    public boolean isStopped() {
        return false;
    }

}
