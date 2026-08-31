/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * The credentials of a tenant's database user, as created by the external provisioner.
 *
 * <p>
 * Only the parts that differ from the application's own default data source are required.
 * Everything else - the URL, the driver, the connection properties - is cloned from that data
 * source, because the tenant lives in the same database as the application; a caller that needs a
 * different one may still override them.
 */
public class TenantDataSourceParameter {

    /** The database user the external provisioner created for this tenant. */
    @NotBlank(message = "A database username is required")
    private String username;

    /** The password of that user. */
    @NotBlank(message = "A database password is required")
    private String password;

    /** The schema the provisioner created for this tenant. */
    @NotBlank(message = "A database schema is required")
    private String schema;

    /** The JDBC URL. Optional - defaults to the URL of the application's default data source. */
    private String url;

    /** The JDBC driver. Optional - defaults to the driver of the application's default data source. */
    private String driver;

    /** Connection properties. Optional - defaults to those of the application's default data source. */
    private List<PropertyParameter> properties;

    /**
     * Whether to open a connection with the supplied credentials before accepting them. On by default:
     * credentials that do not work are otherwise discovered by the activation that follows, where the
     * failure looks like a broken application rather than a wrong password.
     */
    private Boolean verifyConnectivity;

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
     * @param username the new username
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
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = password;
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
     * @param url the new url
     */
    public void setUrl(String url) {
        this.url = url;
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
     * @param driver the new driver
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public List<PropertyParameter> getProperties() {
        return properties;
    }

    /**
     * Sets the properties.
     *
     * @param properties the new properties
     */
    public void setProperties(List<PropertyParameter> properties) {
        this.properties = properties;
    }

    /**
     * Whether connectivity should be verified. Defaults to true when the caller says nothing.
     *
     * @return true, if connectivity should be verified
     */
    public boolean isVerifyConnectivity() {
        return verifyConnectivity == null || verifyConnectivity;
    }

    /**
     * Sets whether connectivity should be verified.
     *
     * @param verifyConnectivity the new value
     */
    public void setVerifyConnectivity(Boolean verifyConnectivity) {
        this.verifyConnectivity = verifyConnectivity;
    }

    /**
     * A connection property.
     */
    public static class PropertyParameter {

        /** The property name. */
        @NotBlank(message = "A property name is required")
        private String name;

        /** The property value. */
        private String value;

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
         * @param value the new value
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}
