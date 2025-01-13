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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class TableConstraints.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLE_CONSTRAINTS")
public class TableConstraints {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONSTRAINTS_ID", nullable = false)
    private Long id;

    /** The primary key. */
    @OneToOne(mappedBy = "constraints", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @Nullable
    @Expose
    private TableConstraintPrimaryKey primaryKey;

    /** The foreign keys. */
    @OneToMany(mappedBy = "constraints", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Nullable
    @Expose
    private List<TableConstraintForeignKey> foreignKeys = new ArrayList<TableConstraintForeignKey>();

    /** The unique indices. */
    @OneToMany(mappedBy = "constraints", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Nullable
    @Expose
    private List<TableConstraintUnique> uniqueIndexes = new ArrayList<TableConstraintUnique>();

    /** The checks. */
    @OneToMany(mappedBy = "constraints", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Nullable
    @Expose
    private List<TableConstraintCheck> checks = new ArrayList<TableConstraintCheck>();

    /** The table. */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "TABLE_ID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Table table;

    /**
     * Instantiates a new table constraints.
     *
     * @param table the table
     */
    public TableConstraints(Table table) {
        this();
        this.table = table;
    }

    /**
     * Instantiates a new table constraints.
     */
    public TableConstraints() {
        super();
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
     * Gets the primary key.
     *
     * @return the primaryKey
     */
    public TableConstraintPrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets the primary key.
     *
     * @param primaryKey the primaryKey to set
     */
    public void setPrimaryKey(TableConstraintPrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Gets the foreign keys.
     *
     * @return the foreignKeys
     */
    public List<TableConstraintForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    /**
     * Sets the foreign keys.
     *
     * @param foreignKeys the foreignKeys to set
     */
    public void setForeignKeys(List<TableConstraintForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    /**
     * Get the foreignKey by name.
     *
     * @param name the name
     * @return the foreignKey
     */
    public TableConstraintForeignKey getForeignKey(String name) {
        final List<TableConstraintForeignKey> foreignKeysList = foreignKeys;
        if (foreignKeysList != null) {
            for (TableConstraintForeignKey fk : foreignKeysList) {
                if (fk.getName()
                      .equals(name)) {
                    return fk;
                }
            }
        }
        return null;
    }

    /**
     * Gets the unique indexes.
     *
     * @return the uniqueIndexes
     */
    public List<TableConstraintUnique> getUniqueIndexes() {
        return uniqueIndexes;
    }

    /**
     * Sets the unique indexes.
     *
     * @param uniqueIndexes the uniqueIndexes to set
     */
    public void setUniqueIndexes(List<TableConstraintUnique> uniqueIndexes) {
        this.uniqueIndexes = uniqueIndexes;
    }

    /**
     * Get the uniqueIndex by name.
     *
     * @param name the name
     * @return the uniqueIndex
     */
    public TableConstraintUnique getUniqueIndex(String name) {
        final List<TableConstraintUnique> uniqueIndexesList = uniqueIndexes;
        if (uniqueIndexesList != null) {
            for (TableConstraintUnique ui : uniqueIndexesList) {
                if (ui.getName()
                      .equals(name)) {
                    return ui;
                }
            }
        }
        return null;
    }

    /**
     * Gets the checks.
     *
     * @return the checks
     */
    public List<TableConstraintCheck> getChecks() {
        return checks;
    }

    /**
     * Sets the checks.
     *
     * @param checks the checks to set
     */
    public void setChecks(List<TableConstraintCheck> checks) {
        this.checks = checks;
    }

    /**
     * Get the checks by name.
     *
     * @param name the name
     * @return the checks
     */
    public TableConstraintCheck getCheck(String name) {
        final List<TableConstraintCheck> checksList = checks;
        if (checksList != null) {
            for (TableConstraintCheck ck : checksList) {
                if (ck.getName()
                      .equals(name)) {
                    return ck;
                }
            }
        }
        return null;
    }

    /**
     * Gets the table.
     *
     * @return the table
     */
    public Table getTable() {
        return table;
    }

    /**
     * Sets the table.
     *
     * @param table the table to set
     */
    public void setTable(Table table) {
        this.table = table;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TableConstraints{" + "id=" + id + ", primaryKey=" + primaryKey + ", foreignKeys=" + foreignKeys + ", uniqueIndexes="
                + uniqueIndexes + ", checks=" + checks + ", table=" + (table != null ? table.getName() : null) + '}';
    }
}
