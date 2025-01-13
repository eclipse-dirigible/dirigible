/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

/**
 * The Class DataSourceProperty.
 */
@Entity
@Table(name = "DIRIGIBLE_DATA_SOURCE_PROPERTIES")
public class DataSourceProperty {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DSP_ID", nullable = false)
    private Long id;

    /** The name. */
    @Column(name = "DSP_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String name;

    /** The default value. */
    @Column(name = "DSP_VALUE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String value;

    /** The table. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "DS_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private DataSource datasource;

    /**
     * Instantiates a new data source property.
     *
     * @param name the name
     * @param value the value
     * @param datasource the datasource
     */
    DataSourceProperty(String name, String value, DataSource datasource) {
        super();
        this.name = name;
        this.value = value;
        this.datasource = datasource;
    }

    /**
     * Instantiates a new data source property.
     */
    public DataSourceProperty() {
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
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the datasource.
     *
     * @return the datasource
     */
    public DataSource getDatasource() {
        return datasource;
    }

    /**
     * Sets the datasource.
     *
     * @param datasource the new datasource
     */
    public void setDatasource(DataSource datasource) {
        this.datasource = datasource;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "DataSourceProperty [id=" + id + ", name=" + name + ", value=" + value + "]";
    }

}
