/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * What the provisioning system decided about one tenant, as typed values - one row per tenant, one
 * column per property.
 *
 * <p>
 * Written only by the provisioning system, each property through the provisioning API that carries
 * it; never by the tenant, whose own settings are its per-tenant configuration. A tenant without a
 * row has none of these properties. A later property is a new nullable column here, with a
 * changeset of its own.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_PROPERTIES")
public class TenantProperties {

    @Id
    @Column(name = "TENANTPROP_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** The most users the tenant may hold at once (#8593); null - no limit. */
    @Column(name = "TENANTPROP_SEAT_LIMIT", columnDefinition = "INTEGER")
    private Integer seatLimit;

    /** Required by JPA. */
    protected TenantProperties() {}

    /**
     * Instantiates the properties of a tenant, none set.
     *
     * @param tenantId the tenant id
     */
    public TenantProperties(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the tenant id.
     *
     * @return the tenant id
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Gets the seat limit: the most users the tenant may hold at once.
     *
     * @return the seat limit, or null for no limit
     */
    public Integer getSeatLimit() {
        return seatLimit;
    }

    /**
     * Sets the seat limit.
     *
     * @param seatLimit the seat limit, at least 1, or null for no limit
     */
    public void setSeatLimit(Integer seatLimit) {
        this.seatLimit = seatLimit;
    }

    @Override
    public String toString() {
        return "TenantProperties [tenantId=" + tenantId + ", seatLimit=" + seatLimit + "]";
    }
}
