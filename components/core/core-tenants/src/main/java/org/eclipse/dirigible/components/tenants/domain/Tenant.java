/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.domain;

import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;

/**
 * A tenant owns/maintains a sub-section of the web application. Can be thought of as a website
 * within the application. Users can register for multiple tenant without them knowing that each
 * separate tenant is part of one and the same application. So the uniqueness of user accounts is
 * determined by the combination of the user's unique ID (= email) combined with the tenant ID.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANTS")
public class Tenant extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "tenant";

    /** The id. */
    @Id
    @Column(name = "TENANT_ID", nullable = false)
    private String id;

    /** The slug. */
    @Column(name = "TENANT_SUBDOMAIN", unique = true, nullable = false)
    private String subdomain;

    /** The status. */
    @Column(name = "TENANT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    /**
     * Instantiates a new tenant.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param subdomain the subdomain
     * @param status the status
     */
    public Tenant(String location, String name, String description, String subdomain, TenantStatus status) {
        super(location, name, ARTEFACT_TYPE, description, null);
        this.subdomain = subdomain;
        this.status = status;
    }

    /**
     * Instantiates a new tenant.
     */
    public Tenant() {
        super();
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the subdomain.
     *
     * @return the subdomain
     */
    public String getSubdomain() {
        return subdomain;
    }

    /**
     * Sets the subdomain.
     *
     * @param subdomain the new subdomain
     */
    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
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
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public TenantStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Tenant [id=" + id + ", subdomain=" + subdomain + ", name=" + name + ", status=" + status + "]";
    }

}
