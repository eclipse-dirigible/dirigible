/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.domain;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.encryption.Encrypted;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class DataSource.
 */
@Entity
@Table(name = "DIRIGIBLE_DATA_SOURCES")
public class DataSource extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "datasource";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DS_ID", nullable = false)
    private Long id;

    /** The driver. */
    @Column(name = "DS_DRIVER", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String driver;

    /** The url. */
    @Column(name = "DS_URL", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String url;

    /** The username. */
    @Column(name = "DS_USERNAME", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String username;

    /** The password. */
    @Column(name = "DS_PASSWORD", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    @Encrypted
    private String password;

    /** The schema. */
    @Column(name = "DS_SCHEMA", columnDefinition = "VARCHAR", nullable = true, length = 64)
    @Expose
    private String schema;

    /** The properties. */
    @OneToMany(mappedBy = "datasource", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Expose
    private List<DataSourceProperty> properties = new ArrayList<>();

    /**
     * Instantiates a new data source.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param driver the driver
     * @param url the url
     * @param username the username
     * @param password the password
     */
    public DataSource(String location, String name, String description, String driver, String url, String username, String password) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Instantiates a new extension.
     */
    public DataSource() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<DataSourceProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<DataSourceProperty> properties) {
        this.properties = properties;
    }

    public DataSourceProperty getProperty(String name) {
        for (DataSourceProperty p : properties) {
            if (p.getName()
                 .equals(name)) {
                return p;
            }
        }
        return null;
    }

    public DataSourceProperty addProperty(String name, String value) {
        DataSourceProperty property = new DataSourceProperty(name, value, this);
        properties.add(property);
        return property;
    }

    @Override
    public String toString() {
        return "DataSource [id=" + id + ", driver=" + driver + ", url=" + url + ", username=" + username + ", schema=" + schema
                + ", properties=" + properties + "]";
    }
}
