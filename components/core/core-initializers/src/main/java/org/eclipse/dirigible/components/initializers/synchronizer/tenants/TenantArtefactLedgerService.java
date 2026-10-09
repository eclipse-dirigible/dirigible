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
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.tenant.TenantArtefactLedger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The {@link TenantArtefactLedger}, kept in the system database, so the outcome of a tenant reads
 * the same from every node of a cluster and survives a restart.
 */
@Service
@Transactional
public class TenantArtefactLedgerService implements TenantArtefactLedger {

    /** The lifecycles that mean the artefact is not in place for the tenant. */
    private static final Set<ArtefactLifecycle> FAILURES = Set.of(ArtefactLifecycle.FAILED, ArtefactLifecycle.FATAL);

    private final TenantArtefactOutcomeRepository repository;

    TenantArtefactLedgerService(TenantArtefactOutcomeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(Artefact artefact, String tenantId, ArtefactLifecycle lifecycle, String error) {
        TenantArtefactOutcome outcome = repository.findByTenantIdAndArtefactKey(tenantId, artefact.getKey())
                                                  .orElseGet(() -> new TenantArtefactOutcome(artefact, tenantId));
        outcome.update(lifecycle, error, Instant.now());
        repository.save(outcome);
    }

    /**
     * The artefacts that failed for a tenant.
     *
     * @param tenantId the tenant id
     * @return the failed outcomes
     */
    @Transactional(readOnly = true)
    public List<TenantArtefactOutcome> findFailures(String tenantId) {
        return repository.findByTenantIdAndLifecycleIn(tenantId, FAILURES);
    }

    /**
     * Forgets an artefact that is gone, for every tenant - a failure of an artefact that no longer
     * exists must not keep a tenant failed.
     *
     * @param artefactKey the artefact key
     */
    public void forget(String artefactKey) {
        repository.deleteByArtefactKey(artefactKey);
    }

    /**
     * Forgets everything recorded for the given tenants, ahead of initializing them again: what a
     * previous initialization left must not outlive the one that replaces it.
     *
     * @param tenantIds the tenant ids
     */
    public void clear(Set<String> tenantIds) {
        repository.deleteByTenantIdIn(tenantIds);
    }
}
