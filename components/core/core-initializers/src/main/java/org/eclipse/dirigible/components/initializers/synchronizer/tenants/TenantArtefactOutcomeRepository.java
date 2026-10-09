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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * The outcomes of the per-tenant artefacts, per tenant.
 */
@Repository
public interface TenantArtefactOutcomeRepository extends JpaRepository<TenantArtefactOutcome, Long> {

    /**
     * Finds the outcome of an artefact for a tenant.
     *
     * @param tenantId the tenant id
     * @param artefactKey the artefact key
     * @return the outcome
     */
    Optional<TenantArtefactOutcome> findByTenantIdAndArtefactKey(String tenantId, String artefactKey);

    /**
     * Finds the outcomes of a tenant in the given lifecycles.
     *
     * @param tenantId the tenant id
     * @param lifecycles the lifecycles
     * @return the outcomes
     */
    List<TenantArtefactOutcome> findByTenantIdAndLifecycleIn(String tenantId, Collection<ArtefactLifecycle> lifecycles);

    /**
     * Deletes the outcomes of an artefact, for every tenant.
     *
     * @param artefactKey the artefact key
     */
    @Modifying
    @Query("delete from TenantArtefactOutcome o where o.artefactKey = :artefactKey")
    void deleteByArtefactKey(String artefactKey);

    /**
     * Deletes the outcomes of the given tenants.
     *
     * @param tenantIds the tenant ids
     */
    @Modifying
    @Query("delete from TenantArtefactOutcome o where o.tenantId in :tenantIds")
    void deleteByTenantIdIn(Collection<String> tenantIds);
}
