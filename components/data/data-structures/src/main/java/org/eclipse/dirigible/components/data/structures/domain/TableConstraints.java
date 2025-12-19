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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONSTRAINTS_ID", nullable = false)
    private Long id;

    /** Primary key (already LAZY – unchanged) */
    @OneToOne(mappedBy = "constraints", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @Nullable
    @Expose
    private TableConstraintPrimaryKey primaryKey;

    @OneToMany(mappedBy = "constraints", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Nullable
    @Expose
    private List<TableConstraintForeignKey> foreignKeys = new ArrayList<>();

    @OneToMany(mappedBy = "constraints", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Nullable
    @Expose
    private List<TableConstraintUnique> uniqueIndexes = new ArrayList<>();

    @OneToMany(mappedBy = "constraints", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Nullable
    @Expose
    private List<TableConstraintCheck> checks = new ArrayList<>();

    /** The table */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "TABLE_ID", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Table table;

    public TableConstraints(Table table) {
        this();
        this.table = table;
    }

    public TableConstraints() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TableConstraintPrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(TableConstraintPrimaryKey primaryKey) {
        this.primaryKey = primaryKey;
    }

    public List<TableConstraintForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<TableConstraintForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    public TableConstraintForeignKey getForeignKey(String name) {
        if (foreignKeys != null) {
            for (TableConstraintForeignKey fk : foreignKeys) {
                if (fk.getName()
                      .equals(name)) {
                    return fk;
                }
            }
        }
        return null;
    }

    public List<TableConstraintUnique> getUniqueIndexes() {
        return uniqueIndexes;
    }

    public void setUniqueIndexes(List<TableConstraintUnique> uniqueIndexes) {
        this.uniqueIndexes = uniqueIndexes;
    }

    public TableConstraintUnique getUniqueIndex(String name) {
        if (uniqueIndexes != null) {
            for (TableConstraintUnique ui : uniqueIndexes) {
                if (ui.getName()
                      .equals(name)) {
                    return ui;
                }
            }
        }
        return null;
    }

    public List<TableConstraintCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<TableConstraintCheck> checks) {
        this.checks = checks;
    }

    public TableConstraintCheck getCheck(String name) {
        if (checks != null) {
            for (TableConstraintCheck ck : checks) {
                if (ck.getName()
                      .equals(name)) {
                    return ck;
                }
            }
        }
        return null;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @Override
    public String toString() {
        return "TableConstraints{" + "id=" + id + ", primaryKey=" + primaryKey + ", foreignKeys=" + foreignKeys + ", uniqueIndexes="
                + uniqueIndexes + ", checks=" + checks + ", table=" + (table != null ? table.getName() : null) + '}';
    }
}
