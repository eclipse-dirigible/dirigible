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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.encryption.Encrypted;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import com.google.gson.annotations.Expose;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
    @Column(name = "DS_USERNAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String username;

    /** The password. */
    @Column(name = "DS_PASSWORD", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    @Encrypted
    private String password;

    /** The schema. */
    @Column(name = "DS_SCHEMA", columnDefinition = "VARCHAR", nullable = true, length = 64)
    @Expose
    private String schema;

    /** The properties. */
    @OneToMany(mappedBy = "datasource", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Expose
    private List<DataSourceProperty> properties = new ArrayList<DataSourceProperty>();

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
     * Gets the driver.
     *
     * @return the driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the driver.
     *
     * @param driver the driver to set
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the properties.
     *
     * @param properties the properties to set
     */
    public void setProperties(List<DataSourceProperty> properties) {
        this.properties = properties;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public List<DataSourceProperty> getProperties() {
        return properties;
    }

    /**
     * Get the property by name.
     *
     * @param name the name
     * @return the property
     */
    public DataSourceProperty getProperty(String name) {
        for (DataSourceProperty p : properties) {
            if (p.getName()
                 .equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Adds the property.
     *
     * @param name the name
     * @param value the value
     * @return the data source property
     */
    public DataSourceProperty addProperty(String name, String value) {
        DataSourceProperty property = new DataSourceProperty(name, value, this);
        properties.add(property);
        return property;
    }

    /**
     * Gets the schema.
     *
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema.
     *
     * @param schema the new schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "DataSource [id=" + id + ", driver=" + driver + ", url=" + url + ", username=" + username + ", schema=" + schema
                + ", properties=" + properties + "]";
    }

}
