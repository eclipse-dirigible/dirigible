/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One export of a tenant: who asked for it, where it stands and where its archive is. The tenant id
 * is a plain column, as the user replica's is - the row lives in the system database and is deleted
 * with the tenant by {@link TenantExportTenantTeardown}.
 */
@Entity
@Table(name = "DIRIGIBLE_TENANT_EXPORTS",
        indexes = {@Index(name = "IX_DIRIGIBLE_TENANT_EXPORTS_TENEXPORT_TENANT_ID", columnList = "TENEXPORT_TENANT_ID")})
public class TenantExport {

    /** The longest error kept. */
    static final int ERROR_LENGTH = 2000;

    /** The export's key in every route. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TENEXPORT_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The tenant the export is of; scopes every route to the current tenant. */
    @Column(name = "TENEXPORT_TENANT_ID", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String tenantId;

    /** Where it stands. */
    @Enumerated(EnumType.STRING)
    @Column(name = "TENEXPORT_STATUS", columnDefinition = "VARCHAR", nullable = false, length = 50)
    private TenantExportStatus status;

    /** Who started it; blanked when the tenant is told to forget that person. */
    @Column(name = "TENEXPORT_REQUESTED_BY", columnDefinition = "VARCHAR", length = 320)
    private String requestedBy;

    /** When it was started. */
    @Column(name = "TENEXPORT_REQUESTED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Instant requestedAt;

    /** When it ended, completed or failed. */
    @Column(name = "TENEXPORT_COMPLETED_AT", columnDefinition = "TIMESTAMP")
    private Instant completedAt;

    /** When the housekeeping deletes it. */
    @Column(name = "TENEXPORT_EXPIRES_AT", columnDefinition = "TIMESTAMP")
    private Instant expiresAt;

    /** The process instance that runs it. */
    @Column(name = "TENEXPORT_PROCESS_INSTANCE_ID", columnDefinition = "VARCHAR", length = 255)
    private String processInstanceId;

    /** Where the archive is in the tenant's CMS. */
    @Column(name = "TENEXPORT_ARCHIVE_PATH", columnDefinition = "VARCHAR", length = 1000)
    private String archivePath;

    /** The archive's size, shown before the download. */
    @Column(name = "TENEXPORT_SIZE_BYTES", columnDefinition = "BIGINT")
    private Long sizeBytes;

    /** Why it failed. */
    @Column(name = "TENEXPORT_ERROR", columnDefinition = "VARCHAR", length = ERROR_LENGTH)
    private String error;

    /**
     * Instantiates an export for JPA.
     */
    TenantExport() {}

    /**
     * Instantiates a requested export.
     *
     * @param tenantId the tenant
     * @param requestedBy who asks
     * @param requestedAt when
     */
    TenantExport(String tenantId, String requestedBy, Instant requestedAt) {
        this.tenantId = tenantId;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.status = TenantExportStatus.REQUESTED;
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public TenantExportStatus getStatus() {
        return status;
    }

    void setStatus(TenantExportStatus status) {
        this.status = status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getArchivePath() {
        return archivePath;
    }

    void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getError() {
        return error;
    }

    void setError(String error) {
        this.error = error == null || error.length() <= ERROR_LENGTH ? error : error.substring(0, ERROR_LENGTH);
    }

    /**
     * Whether the export still runs or waits to.
     *
     * @return true while it is requested or running
     */
    boolean isActive() {
        return status == TenantExportStatus.REQUESTED || status == TenantExportStatus.RUNNING;
    }
}
