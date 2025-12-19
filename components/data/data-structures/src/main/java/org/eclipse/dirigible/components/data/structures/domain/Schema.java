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

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Class Schema.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_SCHEMAS")
public class Schema extends Artefact {

    public static final String ARTEFACT_TYPE = "schema";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEMA_ID", nullable = false)
    private Long id;

    @Column(name = "datasource", nullable = false)
    @Expose
    private String datasource;

    @OneToMany(mappedBy = "schemaReference", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Expose
    private List<Table> tables = new ArrayList<>();

    @OneToMany(mappedBy = "schemaReference", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Expose
    private List<View> views = new ArrayList<>();

    public Schema(String location, String name, String description, Set<String> dependencies) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
    }

    public Schema() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Table findTable(String name) {
        for (Table t : getTables()) {
            if (t.getName()
                 .equals(name)) {
                return t;
            }
        }
        return null;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public View findView(String name) {
        for (View v : getViews()) {
            if (v.getName()
                 .equals(name)) {
                return v;
            }
        }
        return null;
    }

    public List<View> getViews() {
        return views;
    }

    public void setViews(List<View> views) {
        this.views = views;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDataSource(String dataSource) {
        this.datasource = dataSource;
    }

    @Override
    public String toString() {
        return "Schema [id=" + id + ", tables=" + tables + ", views=" + views + ", datasource=" + datasource + ", location=" + location
                + ", name=" + name + ", type=" + type + ", description=" + description + ", key=" + key + ", dependencies=" + dependencies
                + ", createdBy=" + createdBy + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }
}
