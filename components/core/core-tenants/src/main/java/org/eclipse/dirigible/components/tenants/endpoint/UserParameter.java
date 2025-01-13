/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.endpoint;

/**
 * The Class DataSourceParameter.
 */
public class UserParameter {

    /** The username. */
    private String username;

    /** The password. */
    private String password;

    /** The tenant. */
    private String tenant;

    /** The roles. */
    private Long[] roles;

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
     * Gets the tenant.
     *
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant.
     *
     * @param tenant the tenant to set
     */
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    /**
     * Gets the roles.
     *
     * @return the roles
     */
    public Long[] getRoles() {
        return roles;
    }

    /**
     * Sets the roles.
     *
     * @param roles the new roles
     */
    public void setRoles(Long[] roles) {
        this.roles = roles;
    }

}
