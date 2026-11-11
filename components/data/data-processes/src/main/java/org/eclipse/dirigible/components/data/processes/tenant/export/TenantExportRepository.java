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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tenant exports.
 */
@Repository("tenantExportRepository")
public interface TenantExportRepository extends JpaRepository<TenantExport, Long> {

    /**
     * The exports of a tenant, newest first.
     *
     * @param tenantId the tenant id
     * @return the exports
     */
    List<TenantExport> findByTenantIdOrderByRequestedAtDescIdDesc(String tenantId);

    /**
     * One export of a tenant.
     *
     * @param id the id
     * @param tenantId the tenant id
     * @return the export
     */
    Optional<TenantExport> findByIdAndTenantId(Long id, String tenantId);

    /**
     * Whether a tenant has an export in one of the statuses.
     *
     * @param tenantId the tenant id
     * @param statuses the statuses
     * @return whether it has
     */
    boolean existsByTenantIdAndStatusIn(String tenantId, Collection<TenantExportStatus> statuses);

    /**
     * How many exports a tenant has in a status.
     *
     * @param tenantId the tenant id
     * @param status the status
     * @return how many
     */
    long countByTenantIdAndStatus(String tenantId, TenantExportStatus status);

    /**
     * The exports of every tenant in one of the statuses.
     *
     * @param statuses the statuses
     * @return the exports
     */
    List<TenantExport> findByStatusIn(Collection<TenantExportStatus> statuses);

    /**
     * The exports of every tenant that expired before the moment.
     *
     * @param moment the moment
     * @return the exports
     */
    List<TenantExport> findByExpiresAtBefore(Instant moment);

    /**
     * Deletes every export row of the tenant - the tenant's deprovisioning.
     *
     * @param tenantId the tenant id
     * @return how many rows were deleted
     */
    @Transactional
    long deleteByTenantId(String tenantId);

    /**
     * Blanks who asked for the tenant's exports a person started - the person's erasure.
     *
     * @param tenantId the tenant id
     * @param email the person's email, lower-cased
     * @return how many rows were changed
     */
    @Modifying
    @Transactional
    @Query("update TenantExport e set e.requestedBy = null where e.tenantId = :tenantId and lower(e.requestedBy) = :email")
    int clearRequestedBy(@Param("tenantId") String tenantId, @Param("email") String email);
}
