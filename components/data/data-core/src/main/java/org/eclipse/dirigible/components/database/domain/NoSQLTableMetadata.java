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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.dirigible.components.database.DatabaseNameNormalizer;
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * The Table Metadata transport object.
 */
public class NoSQLTableMetadata {

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The remarks. */
    private String remarks;

    /** The columns. */
    private List<NoSQLColumnMetadata> columns;

    /** The kind. */
    private String kind = "collection";

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
    public NoSQLTableMetadata(String name, String type, String remarks, Connection connection, String catalogName, String schemaName,
            boolean deep) throws SQLException {
        super();
        this.name = name;
        this.type = type;
        this.remarks = remarks;

        this.columns = new ArrayList<NoSQLColumnMetadata>();

        DatabaseMetaData dmd = connection.getMetaData();

        ResultSet rs = dmd.getColumns(catalogName, schemaName, DatabaseNameNormalizer.normalizeTableName(name), null);
        if (columns == null) {
            throw new SQLException("DatabaseMetaData.getColumns returns null");
        }

        try {

            while (rs.next()) {
                NoSQLColumnMetadata column = new NoSQLColumnMetadata(rs.getString(DatabaseMetadataHelper.COLUMN_NAME),
                        rs.getString(DatabaseMetadataHelper.TYPE_NAME), rs.getInt(DatabaseMetadataHelper.COLUMN_SIZE),
                        rs.getBoolean(DatabaseMetadataHelper.IS_NULLABLE), rs.getBoolean(DatabaseMetadataHelper.PK),
                        rs.getInt(DatabaseMetadataHelper.DECIMAL_DIGITS));
                if (rs.getObject("NESTED") != null) {
                    Iterator<JsonNode> nestedNodes = ((ArrayNode) rs.getObject("NESTED")).iterator();
                    populateNestedColumns(column, nestedNodes);
                }
                columns.add(column);
            }

        } finally {
            rs.close();
        }

    }

    /**
     * Populate nested columns.
     *
     * @param parent the parent
     * @param nestedNodes the nested nodes
     */
    private void populateNestedColumns(NoSQLColumnMetadata parent, Iterator<JsonNode> nestedNodes) {
        List<NoSQLColumnMetadata> columns = new ArrayList<NoSQLColumnMetadata>();

        while (nestedNodes.hasNext()) {
            JsonNode node = nestedNodes.next();
            NoSQLColumnMetadata column = new NoSQLColumnMetadata(node.get(DatabaseMetadataHelper.COLUMN_NAME)
                                                                     .asText(),
                    node.get(DatabaseMetadataHelper.TYPE_NAME)
                        .asText(),
                    node.get(DatabaseMetadataHelper.COLUMN_SIZE)
                        .asInt(),
                    node.get(DatabaseMetadataHelper.IS_NULLABLE)
                        .asBoolean(),
                    node.get(DatabaseMetadataHelper.PK)
                        .asBoolean(),
                    node.get(DatabaseMetadataHelper.DECIMAL_DIGITS)
                        .asInt());
            if (node.get("NESTED") != null) {
                Iterator<JsonNode> nestedIterator = node.get("NESTED")
                                                        .iterator();
                populateNestedColumns(column, nestedIterator);
            }
            columns.add(column);
        }
        parent.setColumns(columns);
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
    public List<NoSQLColumnMetadata> getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the new columns
     */
    public void setColumns(List<NoSQLColumnMetadata> columns) {
        this.columns = columns;
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
