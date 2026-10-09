/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps the {@link TenantInitialization} of each activated tenant.
 */
@Service
@Transactional
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantInitializationService {

    private final TenantInitializationRepository repository;

    TenantInitializationService(TenantInitializationRepository repository) {
        this.repository = repository;
    }

    /**
     * Opens the initialization of a tenant, replacing whatever an earlier activation left.
     *
     * @param tenantId the tenant id
     */
    void request(String tenantId) {
        TenantInitialization initialization = repository.findById(tenantId)
                                                        .orElseGet(() -> new TenantInitialization(tenantId));
        initialization.request(Instant.now());
        repository.save(initialization);
    }

    /**
     * Marks the initialization of a tenant as running.
     *
     * @param tenantId the tenant id
     */
    void start(String tenantId) {
        repository.findById(tenantId)
                  .ifPresent(initialization -> {
                      initialization.start(Instant.now());
                      repository.save(initialization);
                  });
    }

    /**
     * Closes the initialization of a tenant.
     *
     * @param tenantId the tenant id
     * @param failure what made the run itself fail, or null
     */
    void finish(String tenantId, String failure) {
        repository.findById(tenantId)
                  .ifPresent(initialization -> {
                      initialization.finish(Instant.now(), failure);
                      repository.save(initialization);
                  });
    }

    /**
     * Finds the initialization of a tenant.
     *
     * @param tenantId the tenant id
     * @return the initialization, empty when the tenant was never activated through the API
     */
    @Transactional(readOnly = true)
    Optional<TenantInitialization> find(String tenantId) {
        return repository.findById(tenantId);
    }

    /**
     * Finds the initializations that have not ended - the ones a restart interrupted.
     *
     * @return the open initializations
     */
    @Transactional(readOnly = true)
    List<TenantInitialization> findOpen() {
        return repository.findByStateIn(Set.of(TenantInitialization.State.REQUESTED, TenantInitialization.State.RUNNING));
    }
}
