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
import org.eclipse.dirigible.components.database.helpers.DatabaseMetadataHelper.FunctionColumnsIteratorCallback;


/**
 * The Function Metadata transport object.
 */
public class FunctionMetadata {

    /** The name. */
    private String name;

    /** The type. */
    private String type;

    /** The remarks. */
    private String remarks;

    /** The columns. */
    private List<ParameterColumnMetadata> columns;

    /** The kind. */
    private String kind = "function";

    /**
     * Instantiates a new function metadata.
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
    public FunctionMetadata(String name, String type, String remarks, Connection connection, String catalogName, String schemaName,
            boolean deep) throws SQLException {
        super();
        this.name = name;
        this.type = type;
        this.remarks = remarks;

        this.columns = new ArrayList<ParameterColumnMetadata>();

        if (deep) {
            DatabaseMetadataHelper.iterateFunctionDefinition(connection, catalogName, schemaName, name,
                    new FunctionColumnsIteratorCallback() {
                        @Override
                        public void onFunctionColumn(String name, int kind, String type, int precision, int length, int scale, int radix,
                                boolean nullable, String remarks) {
                            columns.add(new ParameterColumnMetadata(name, kind, type, precision, length, scale, radix, nullable, remarks));
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
     * Get the metadata columns.
     *
     * @return the columns
     */
    public List<ParameterColumnMetadata> getColumns() {
        return columns;
    }

    /**
     * Set the metadata columns.
     *
     * @param columns the columns to set
     */
    public void setColumns(List<ParameterColumnMetadata> columns) {
        this.columns = columns;
    }

    /**
     * Get the metadata kind.
     *
     * @return the kind
     */
    public String getKind() {
        return kind;
    }

    /**
     * Set the metadata kind.
     *
     * @param kind the kind
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

}
