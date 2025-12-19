/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Class Table.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLES")
public class Table extends Artefact {

    public static final String ARTEFACT_TYPE = "table";
    @Column(name = "TABLE_KIND", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String kind;
    @Column(name = "TABLE_SCHEMA", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    protected String schema;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TABLE_ID", nullable = false)
    private Long id;

    @OneToMany(mappedBy = "table", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Expose
    private List<TableColumn> columns = new ArrayList<>();

    @OneToMany(mappedBy = "table", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Nullable
    @Expose
    private List<TableIndex> indexes = new ArrayList<>();

    @OneToOne(mappedBy = "table", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true, orphanRemoval = true)
    @Nullable
    @Expose
    private TableConstraints constraints;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "SCHEMA_ID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Schema schemaReference;

    public Table(String tableName) {
        this(tableName, tableName, null, null, "TABLE", "");
    }

    public Table(String location, String name, String description, Set<String> dependencies, String kind, String schema) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.constraints = new TableConstraints(this);
        this.kind = kind;
        this.schema = schema;
    }

    public Table() {
        super();
        this.constraints = new TableConstraints();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }

    public TableColumn getColumn(String name) {
        for (TableColumn c : columns) {
            if (c.getName()
                 .equals(name)) {
                return c;
            }
        }
        return null;
    }

    public List<TableIndex> getIndexes() {
        return indexes;
    }

    public void setIndexes(List<TableIndex> indexes) {
        this.indexes = indexes;
    }

    public TableIndex getIndex(String name) {
        if (indexes != null) {
            for (TableIndex i : indexes) {
                if (i.getName()
                     .equals(name)) {
                    return i;
                }
            }
        }
        return null;
    }

    public TableConstraints getConstraints() {
        return constraints;
    }

    public void setConstraints(TableConstraints constraints) {
        this.constraints = constraints;
    }

    public Schema getSchemaReference() {
        return schemaReference;
    }

    public void setSchemaReference(Schema schemaReference) {
        this.schemaReference = schemaReference;
    }

    @Override
    public String toString() {
        return "Table [id=" + id + ", schemaName=" + schema + ", columns=" + columns + ", indexes=" + indexes + ", constraints="
                + constraints + ", location=" + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key="
                + key + ", dependencies=" + dependencies + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy="
                + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

    public TableColumn addColumn(String name, String type, String length, boolean nullable, boolean primaryKey, String defaultValue,
            String precision, String scale, boolean unique) {
        TableColumn tableColumn = new TableColumn(name, type, length, nullable, primaryKey, defaultValue, precision, scale, unique, this);
        columns.add(tableColumn);
        return tableColumn;
    }

    public TableIndex addIndex(String name, String type, boolean unique, String order, String[] columns) {
        TableIndex tableIndex = new TableIndex(name, type, unique, order, columns, this);
        indexes.add(tableIndex);
        return tableIndex;
    }
}
