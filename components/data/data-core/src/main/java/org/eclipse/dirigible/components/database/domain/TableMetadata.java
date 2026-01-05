/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.database.domain;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper.ColumnsIteratorCallback;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper.ForeignKeysIteratorCallback;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper.IndicesIteratorCallback;

/**
 * The Table Metadata transport object.
 */
public class TableMetadata {

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The remarks. */
    private String remarks;

    /** The columns. */
    private List<ColumnMetadata> columns;

    /** The indices. */
    private List<IndexMetadata> indices;

    /** The indices. */
    private List<ForeignKeyMetadata> foreignKeys;

    /** The kind. */
    private String kind = "table";

    /**
     * Instantiates a new table metadata.
     *
     * @param name the name
     * @param type the type
     * @param remarks the remarks
     * @param connection the connection
     * @param catalogName the catalog name
     * @param schemaName the schema name
     * @param deep whether to populate also the columns
     * @throws SQLException the SQL exception
     */
    public TableMetadata(String name, String type, String remarks, Connection connection, String catalogName, String schemaName,
            boolean deep) throws SQLException {
        super();
        this.name = name;
        this.type = type;
        this.remarks = remarks;

        this.columns = new ArrayList<ColumnMetadata>();
        this.indices = new ArrayList<IndexMetadata>();
        this.foreignKeys = new ArrayList<ForeignKeyMetadata>();

        if (deep) {
            DatabaseMetadataHelper.iterateTableDefinition(connection, catalogName, schemaName, name, new ColumnsIteratorCallback() {
                @Override
                public void onColumn(String columnName, String columnType, String columnSize, boolean isNullable, boolean isKey,
                        int scale) {
                    columns.add(new ColumnMetadata(columnName, columnType, columnSize != null ? Integer.parseInt(columnSize) : 0,
                            isNullable, isKey, scale));
                }
            }, new IndicesIteratorCallback() {
                @Override
                public void onIndex(String indexName, String indexType, String columnName, boolean isNonUnique, String indexQualifier,
                        String ordinalPosition, String sortOrder, String cardinality, String pagesIndex, String filterCondition) {
                    indices.add(new IndexMetadata(indexName, indexType, columnName, isNonUnique, indexQualifier, ordinalPosition, sortOrder,
                            cardinality != null ? Integer.parseInt(cardinality) : 0, pagesIndex != null ? Integer.parseInt(pagesIndex) : 0,
                            filterCondition));
                }
            }, new ForeignKeysIteratorCallback() {
                @Override
                public void onIndex(String fkName) {
                    foreignKeys.add(new ForeignKeyMetadata(fkName));
                }
            });
        }
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the new type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the remarks.
     *
     * @return the remarks
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Sets the remarks.
     *
     * @param remarks the new remarks
     */
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    /**
     * Gets the indices.
     *
     * @return the indices
     */
    public List<IndexMetadata> getIndices() {
        return indices;
    }

    /**
     * Gets the kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind.
     *
     * @param kind the new kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

}
