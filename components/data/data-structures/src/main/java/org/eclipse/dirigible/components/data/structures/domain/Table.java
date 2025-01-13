/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

/**
 * The Class Table.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLES")
public class Table extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "table";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TABLE_ID", nullable = false)
    private Long id;

    /** The kind. */
    @Column(name = "TABLE_KIND", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String kind;

    /** The schema name. */
    @Column(name = "TABLE_SCHEMA", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String schema;

    /** The columns. */
    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Expose
    private List<TableColumn> columns = new ArrayList<TableColumn>();

    /** The indexes. */
    @OneToMany(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Nullable
    @Expose
    private List<TableIndex> indexes = new ArrayList<TableIndex>();

    /** The constraints. */
    @OneToOne(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true, orphanRemoval = true)
    @Nullable
    @Expose
    private TableConstraints constraints;

    /** The schema reference. */
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "SCHEMA_ID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Schema schemaReference;

    /**
     * Instantiates a new table.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param kind the kind
     * @param schema the schema name
     */
    public Table(String location, String name, String description, Set<String> dependencies, String kind, String schema) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.constraints = new TableConstraints(this);
        this.kind = kind;
        this.schema = schema;
    }

    /**
     * Instantiates a new table.
     *
     * @param tableName the table name
     */
    public Table(String tableName) {
        this(tableName, tableName, null, null, "TABLE", "");
    }

    /**
     * Instantiates a new table.
     */
    public Table() {
        super();
        this.constraints = new TableConstraints();
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
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
     * @param kind the kind to set
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Gets the schema name.
     *
     * @return the schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema name.
     *
     * @param schema the schema name to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public List<TableColumn> getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the columns to set
     */
    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }

    /**
     * Get the column by name.
     *
     * @param name the name
     * @return the column
     */
    public TableColumn getColumn(String name) {
        for (TableColumn c : columns) {
            if (c.getName()
                 .equals(name)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Gets the indexes.
     *
     * @return the indexes
     */
    public List<TableIndex> getIndexes() {
        return indexes;
    }

    /**
     * Sets the indexes.
     *
     * @param indexes the indexes to set
     */
    public void setIndexes(List<TableIndex> indexes) {
        this.indexes = indexes;
    }

    /**
     * Get the index by name.
     *
     * @param name the name
     * @return the index
     */
    public TableIndex getIndex(String name) {
        final List<TableIndex> indexesList = indexes;
        if (indexesList != null) {
            for (TableIndex i : indexesList) {
                if (i.getName()
                     .equals(name)) {
                    return i;
                }
            }
        }
        return null;
    }

    /**
     * Gets the constraints.
     *
     * @return the constraints
     */
    public TableConstraints getConstraints() {
        return constraints;
    }

    /**
     * Sets the constraints.
     *
     * @param constraints the constraints to set
     */
    public void setConstraints(TableConstraints constraints) {
        this.constraints = constraints;
    }

    /**
     * Gets the schema reference.
     *
     * @return the schema reference
     */
    public Schema getSchemaReference() {
        return schemaReference;
    }

    /**
     * Sets the schema reference.
     *
     * @param schemaReference the new schema reference
     */
    public void setSchemaReference(Schema schemaReference) {
        this.schemaReference = schemaReference;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Table [id=" + id + ", schemaName=" + schema + ", columns=" + columns + ", indexes=" + indexes + ", constraints="
                + constraints + ", location=" + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key="
                + key + ", dependencies=" + dependencies + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy="
                + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

    /**
     * Adds the column.
     *
     * @param name the name
     * @param type the type
     * @param length the length
     * @param nullable the nullable
     * @param primaryKey the primary key
     * @param defaultValue the default value
     * @param scale the scale
     * @param unique the unique
     * @return the table column
     */
    public TableColumn addColumn(String name, String type, String length, boolean nullable, boolean primaryKey, String defaultValue,
            String scale, boolean unique) {
        TableColumn tableColumn = new TableColumn(name, type, length, nullable, primaryKey, defaultValue, scale, unique, this);
        columns.add(tableColumn);
        return tableColumn;
    }

    /**
     * Adds the index.
     *
     * @param name the name
     * @param type the type
     * @param unique the unique
     * @param order the order
     * @param columns the columns
     * @return the table index
     */
    public TableIndex addIndex(String name, String type, boolean unique, String order, String[] columns) {
        TableIndex tableIndex = new TableIndex(name, type, unique, order, columns, this);
        indexes.add(tableIndex);
        return tableIndex;
    }



}
