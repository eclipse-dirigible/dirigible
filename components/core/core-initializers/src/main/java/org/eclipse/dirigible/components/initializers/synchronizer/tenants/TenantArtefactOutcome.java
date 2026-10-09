/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer.tenants;

import java.time.Instant;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The outcome of one per-tenant artefact for one tenant - a row of the
 * {@link org.eclipse.dirigible.components.base.tenant.TenantArtefactLedger}.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_ARTEFACTS", uniqueConstraints = {
        @UniqueConstraint(name = "UK_DIRIGIBLE_TENANT_ARTEFACTS_TENANT_KEY", columnNames = {"TENART_TENANT_ID", "TENART_ARTEFACT_KEY"})})
public class TenantArtefactOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENART_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    @Column(name = "TENART_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    @Column(name = "TENART_ARTEFACT_KEY", columnDefinition = "VARCHAR", nullable = false, length = 2000)
    private String artefactKey;

    @Column(name = "TENART_ARTEFACT_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String artefactType;

    @Column(name = "TENART_ARTEFACT_LOCATION", columnDefinition = "VARCHAR", nullable = false, length = 2000)
    private String artefactLocation;

    @Column(name = "TENART_LIFECYCLE", columnDefinition = "VARCHAR", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private ArtefactLifecycle lifecycle;

    @Column(name = "TENART_ERROR", columnDefinition = "VARCHAR", length = Artefact.ERROR_LENGTH)
    private String error;

    @Column(name = "TENART_UPDATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected TenantArtefactOutcome() {}

    TenantArtefactOutcome(Artefact artefact, String tenantId) {
        this.tenantId = tenantId;
        this.artefactKey = artefact.getKey();
        this.artefactType = artefact.getType();
        this.artefactLocation = artefact.getLocation();
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
     * Gets the artefact key.
     *
     * @return the artefact key
     */
    public String getArtefactKey() {
        return artefactKey;
    }

    /**
     * Gets the artefact type.
     *
     * @return the artefact type
     */
    public String getArtefactType() {
        return artefactType;
    }

    /**
     * Gets the artefact location.
     *
     * @return the artefact location
     */
    public String getArtefactLocation() {
        return artefactLocation;
    }

    /**
     * Gets the lifecycle the artefact reached for the tenant.
     *
     * @return the lifecycle
     */
    public ArtefactLifecycle getLifecycle() {
        return lifecycle;
    }

    /**
     * Gets the error, when the lifecycle is a failure.
     *
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * Gets when the outcome was recorded.
     *
     * @return the time
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    void update(ArtefactLifecycle lifecycle, String error, Instant updatedAt) {
        this.lifecycle = lifecycle;
        this.error = error != null && error.length() > Artefact.ERROR_LENGTH ? error.substring(0, Artefact.ERROR_LENGTH) : error;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TenantArtefactOutcome [tenantId=" + tenantId + ", artefactKey=" + artefactKey + ", lifecycle=" + lifecycle + "]";
    }
}
