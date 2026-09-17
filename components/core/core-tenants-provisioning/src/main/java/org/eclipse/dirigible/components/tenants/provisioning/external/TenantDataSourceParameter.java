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

import jakarta.validation.constraints.NotBlank;

/**
 * The credentials of a tenant's database user, as created by the external provisioner.
 *
 * <p>
 * Only the credentials. Everything else that makes up a data source - the URL, the driver, the
 * connection properties - is cloned from the application's own default data source, exactly as the
 * built-in provisioner clones it, because the tenant lives in the same database as the application.
 *
 * <p>
 * That is a security boundary, not only a convenience. A JDBC URL and driver properties are
 * executable surface: an H2 URL can carry {@code INIT=RUNSCRIPT FROM '<url>'} and a PostgreSQL
 * connection property can name a {@code socketFactory} class, so accepting either from a caller
 * would turn "may register a tenant's credentials" into "may run code on the application server".
 * They are therefore not part of this API at all, rather than validated - a caller that sends them
 * is ignored.
 *
 * <p>
 * There is no way to skip the connectivity check either. An earlier draft offered one, which could
 * not be honoured: saving a data source definition initializes its pool through a lifecycle
 * listener, so a caller that opted out still had the connection attempted - and got an unhandled
 * 500 instead of the 502 the check answers with.
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
}
