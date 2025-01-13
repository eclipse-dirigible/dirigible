/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import org.eclipse.dirigible.components.base.tenant.Tenant;

import java.util.Objects;

/**
 * The Class TenantImpl.
 */
class TenantImpl implements Tenant {

    /** The Constant DEFAULT_TENANT_ID. */
    private static final String DEFAULT_TENANT_ID = "default-tenant";

    /** The Constant DEFAULT_TENANT_NAME. */
    private static final String DEFAULT_TENANT_NAME = "default-tenant";

    /** The Constant DEFAULT_TENANT_SUBDOMAIN. */
    private static final String DEFAULT_TENANT_SUBDOMAIN = "default";

    /** The Constant DEFAULT_TENANT. */
    private static final Tenant DEFAULT_TENANT = new TenantImpl(DEFAULT_TENANT_ID, DEFAULT_TENANT_NAME, DEFAULT_TENANT_SUBDOMAIN);

    /** The id. */
    private final String id;

    /** The name. */
    private final String name;

    /** The subdomain. */
    private final String subdomain;

    /** The default tenant. */
    private final boolean defaultTenant;

    /**
     * Instantiates a new tenant impl.
     *
     * @param id the id
     * @param name the name
     * @param subdomain the subdomain
     */
    TenantImpl(String id, String name, String subdomain) {
        this.id = id;
        this.name = name;
        this.subdomain = subdomain;
        this.defaultTenant = Objects.equals(id, DEFAULT_TENANT_ID);
    }

    /**
     * Gets the default tenant.
     *
     * @return the default tenant
     */
    static Tenant getDefaultTenant() {
        return DEFAULT_TENANT;
    }

    /**
     * Creates the from entity.
     *
     * @param tenant the tenant
     * @return the tenant
     */
    static Tenant createFromEntity(org.eclipse.dirigible.components.tenants.domain.Tenant tenant) {
        return new TenantImpl(tenant.getId(), tenant.getName(), tenant.getSubdomain());
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Checks if is default.
     *
     * @return true, if is default
     */
    @Override
    public boolean isDefault() {
        return defaultTenant;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the subdomain.
     *
     * @return the subdomain
     */
    @Override
    public String getSubdomain() {
        return subdomain;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TenantImpl [id=" + id + ", name=" + name + ", subdomain=" + subdomain + ", defaultTenant=" + defaultTenant + "]";
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TenantImpl other = (TenantImpl) obj;
        return Objects.equals(id, other.id);
    }
}
