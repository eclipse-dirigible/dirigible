/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

/**
 * The tenant users replica. Every lookup carries the tenant id, so a tenant can never reach another
 * tenant's users.
 */
@Repository("tenantUserRepository")
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {

    /**
     * One user of a tenant, removed or not.
     *
     * @param tenantId the tenant id
     * @param email the email, normalized
     * @return the user
     */
    Optional<TenantUser> findByTenantIdAndEmail(String tenantId, String email);

    /**
     * One user of a tenant, locked for the rest of the transaction - so two snapshots of the same user
     * are applied one after the other, each against what the other wrote.
     *
     * @param tenantId the tenant id
     * @param email the email, normalized
     * @return the user
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<TenantUser> findForUpdateByTenantIdAndEmail(String tenantId, String email);

    /**
     * One user of a tenant by id - how the Owner's commands address a user.
     *
     * @param id the id
     * @param tenantId the tenant id
     * @return the user
     */
    Optional<TenantUser> findByIdAndTenantId(Long id, String tenantId);

    /**
     * The users of a tenant other than the given status, by email - with
     * {@link TenantUserStatus#REMOVED}, the live users.
     *
     * @param tenantId the tenant id
     * @param status the status to leave out
     * @return the users
     */
    List<TenantUser> findByTenantIdAndStatusNotOrderByEmail(String tenantId, TenantUserStatus status);

    /**
     * Every user of a tenant by email, the tombstones included - for diagnostics.
     *
     * @param tenantId the tenant id
     * @return the users
     */
    List<TenantUser> findByTenantIdOrderByEmail(String tenantId);

    /**
     * The highest revision stored for a tenant - the replica's watermark.
     *
     * @param tenantId the tenant id
     * @return the revision, or null when the tenant has no users
     */
    @Query("select max(u.revision) from TenantUser u where u.tenantId = :tenantId")
    Long findMaxRevision(@Param("tenantId") String tenantId);
}
