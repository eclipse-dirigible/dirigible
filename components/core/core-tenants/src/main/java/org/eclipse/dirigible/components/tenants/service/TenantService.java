/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.springframework.stereotype.Service;

/**
 * The Class TenantService.
 */
@Service
public class TenantService {

    /** The tenant repository. */
    private final TenantRepository tenantRepository;

    /**
     * Instantiates a new tenant service.
     *
     * @param tenantRepository the tenant repository
     */
    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    public final List<Tenant> getAll() {
        return tenantRepository.findAll();
    }

    /**
     * Find by subdomain.
     *
     * @param subdomain the subdomain
     * @return the optional
     */
    public Optional<Tenant> findBySubdomain(String subdomain) {
        return tenantRepository.findBySubdomain(subdomain);
    }

    /**
     * Find by status.
     *
     * @param status the status
     * @return the sets the
     */
    public Set<Tenant> findByStatus(TenantStatus status) {
        return tenantRepository.findByStatus(status);
    }

    /**
     * Save.
     *
     * @param tenant the tenant
     * @return the tenant
     */
    public Tenant save(Tenant tenant) {
        return tenantRepository.save(tenant);
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the optional
     */
    public Optional<Tenant> findById(String id) {
        return tenantRepository.findById(id);
    }

    /**
     * Delete.
     *
     * @param tenant the tenant
     */
    public void delete(Tenant tenant) {
        tenantRepository.delete(tenant);
    }

}
