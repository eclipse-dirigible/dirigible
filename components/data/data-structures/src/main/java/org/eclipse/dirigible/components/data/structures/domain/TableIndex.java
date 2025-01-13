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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import org.eclipse.dirigible.components.base.converters.ArrayOfStringsToCsvConverter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

/**
 * The Class TableIndex.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLE_INDEXES")
public class TableIndex {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INDEX_ID", nullable = false)
    private Long id;

    /** The name. */
    @Column(name = "INDEX_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String name;

    /** The type. */
    @Column(name = "INDEX_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String type;

    /** The unique. */
    @Column(name = "INDEX_UNIQUE", columnDefinition = "BOOLEAN", nullable = true)
    @Expose
    private boolean unique;

    /** The order. */
    @Column(name = "INDEX_ORDER", columnDefinition = "VARCHAR", nullable = true, length = 20)
    @Expose
    private String order;

    /** The index columns. */
    @Column(name = "INDEX_COLUMNS", columnDefinition = "VARCHAR", nullable = false, length = 2000)
    @Convert(converter = ArrayOfStringsToCsvConverter.class)
    @Expose
    private String[] columns;

    /** The table. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "TABLE_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Table table;

    /**
     * Instantiates a new table index.
     *
     * @param name the name
     * @param type the type
     * @param unique the unique
     * @param order the order
     * @param columns the columns
     * @param table the table
     */
    public TableIndex(String name, String type, boolean unique, String order, String[] columns, Table table) {
        super();
        this.name = name;
        this.type = type;
        this.unique = unique;
        this.order = order;
        this.columns = columns;
        this.table = table;
        this.table.getIndexes()
                  .add(this);
    }

    /**
     * Instantiates a new table index.
     */
    public TableIndex() {
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
     * @param name the name to set
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
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Checks if is unique.
     *
     * @return the unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Sets the unique.
     *
     * @param unique the unique to set
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * Gets the order.
     *
     * @return the order
     */
    public String getOrder() {
        return order;
    }

    /**
     * Sets the order.
     *
     * @param order the order to set
     */
    public void setOrder(String order) {
        this.order = order;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public String[] getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the columns to set
     */
    public void setColumns(String[] columns) {
        this.columns = columns;
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
        return "TableIndex [id=" + id + ", name=" + name + ", type=" + type + ", unique=" + unique + ", order=" + order + ", columns="
                + columns + ", table=" + table.getName() + "]";
    }

}
