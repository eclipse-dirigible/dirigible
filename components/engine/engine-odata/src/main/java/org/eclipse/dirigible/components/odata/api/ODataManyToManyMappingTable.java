/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.odata.api;

import com.google.gson.annotations.Expose;

/**
 * The Class ODataManyToManyMappingTableDefinition.
 */
public class ODataManyToManyMappingTable {

    /** The mapping table name. */
    @Expose
    private String mappingTableName;

    /** The mapping table join column. */
    @Expose
    private String mappingTableJoinColumn;

    /**
     * Gets the mapping table name.
     *
     * @return the mapping table name
     */
    public String getMappingTableName() {
        return mappingTableName;
    }

    /**
     * Sets the mapping table name.
     *
     * @param mappingTableName the new mapping table name
     */
    public void setMappingTableName(String mappingTableName) {
        this.mappingTableName = mappingTableName;
    }

    /**
     * Gets the mapping table join column.
     *
     * @return the mapping table join column
     */
    public String getMappingTableJoinColumn() {
        return mappingTableJoinColumn;
    }

    /**
     * Sets the mapping table join column.
     *
     * @param mappingTableJoinColumn the new mapping table join column
     */
    public void setMappingTableJoinColumn(String mappingTableJoinColumn) {
        this.mappingTableJoinColumn = mappingTableJoinColumn;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "ODataManyToManyMappingTable [mappingTableName=" + mappingTableName + ", mappingTableJoinColumn=" + mappingTableJoinColumn
                + "]";
    }

}
