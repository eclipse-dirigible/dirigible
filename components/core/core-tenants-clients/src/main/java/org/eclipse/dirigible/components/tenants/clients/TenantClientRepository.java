/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

/**
 * The integration client registry. Every lookup but the one by client id carries the tenant id.
 */
@Repository("tenantClientRepository")
public interface TenantClientRepository extends JpaRepository<TenantClient, Long> {

    /**
     * A client by its client id, whichever tenant it belongs to.
     *
     * @param clientId the client id
     * @return the client
     */
    Optional<TenantClient> findByClientId(String clientId);

    /**
     * The clients of a tenant, locked for the registry being applied.
     *
     * @param tenantId the tenant id
     * @return the clients
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TenantClient> findForUpdateByTenantIdOrderByClientId(String tenantId);

    /**
     * The clients of a tenant.
     *
     * @param tenantId the tenant id
     * @return the clients, by client id
     */
    List<TenantClient> findByTenantIdOrderByClientId(String tenantId);

    /**
     * The revision of the registry last applied to a tenant.
     *
     * @param tenantId the tenant id
     * @return the revision, null when the tenant has no client
     */
    @Query("select max(c.revision) from TenantClient c where c.tenantId = :tenantId")
    Long findMaxRevision(@Param("tenantId") String tenantId);

    /**
     * Deletes the clients of a tenant, their roles through the cascade.
     *
     * @param tenantId the tenant id
     * @return how many were deleted
     */
    @Transactional
    long deleteByTenantId(String tenantId);
}
