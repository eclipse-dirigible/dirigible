/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.transfer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.artefact.topology.TopologicallySortable;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableModel;
import org.eclipse.dirigible.database.persistence.model.PersistenceTableRelationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DataTransferSortableTableWrapper.
 */
public class DataTransferSortableTableWrapper implements TopologicallySortable {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DataTransferSortableTableWrapper.class);

    /** The table model. */
    private PersistenceTableModel tableModel;

    /** The wrappers. */
    Map<String, DataTransferSortableTableWrapper> wrappers;

    /**
     * Instantiates a new data transfer sortable table wrapper.
     *
     * @param tableModel the table model
     * @param wrappers the wrappers
     */
    public DataTransferSortableTableWrapper(PersistenceTableModel tableModel, Map<String, DataTransferSortableTableWrapper> wrappers) {
        this.tableModel = tableModel;
        this.wrappers = wrappers;
        this.wrappers.put(getId(), this);
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return tableModel.getTableName();
    }

    /**
     * Gets the dependencies.
     *
     * @return the dependencies
     */
    @Override
    public List<TopologicallySortable> getDependencies() {
        List<TopologicallySortable> dependencies = new ArrayList<TopologicallySortable>();
        for (PersistenceTableRelationModel dependency : this.tableModel.getRelations()) {
            String dependencyName = dependency.getToTableName();
            if (!wrappers.containsKey(dependencyName)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Dependency is not present in this cycle: " + dependencyName);
                }
            } else {
                dependencies.add(wrappers.get(dependencyName));
            }
        }
        return dependencies;
    }

    /**
     * Gets the table model.
     *
     * @return the table model
     */
    public PersistenceTableModel getTableModel() {
        return tableModel;
    }

}
