/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.tenant;

import org.eclipse.dirigible.components.base.tenant.*;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class TenantContextImpl implements TenantContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantContextImpl.class);

    private static final ThreadLocal<Tenant> currentTenantHolder = new ThreadLocal<>();

    private final TenantService tenantService;

    TenantContextImpl(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public boolean isNotInitialized() {
        return !isInitialized();
    }

    @Override
    public boolean isInitialized() {
        return null != currentTenantHolder.get();
    }

    @Override
    public <Result, Exc extends Throwable> Result execute(String tenantId, CallableResultAndException<Result, Exc> callable)
            throws TenantNotFoundException, Exc {
        org.eclipse.dirigible.components.tenants.domain.Tenant tenantEntity = tenantService.findById(tenantId)
                                                                                           .orElseThrow(() -> new TenantNotFoundException(
                                                                                                   tenantId));
        Tenant tenant = TenantImpl.createFromEntity(tenantEntity);
        return execute(tenant, callable);
    }

    @Override
    public <Result, Exc extends Throwable> Result execute(Tenant tenant, CallableResultAndException<Result, Exc> callable) throws Exc {
        Tenant currentTenant = safelyGetCurrentTenant();
        setCurrentTenant(tenant);
        try {
            return callable.call();
        } finally {
            setCurrentTenant(currentTenant);
        }
    }

    private Tenant safelyGetCurrentTenant() {
        return isInitialized() ? getCurrentTenant() : null;
    }

    @Override
    public Tenant getCurrentTenant() {
        Tenant tenant = currentTenantHolder.get();
        if (null == tenant) {
            throw new IllegalStateException("Attempting to get current tenant but it is not initialized yet.");
        }
        LOGGER.debug("Getting current tenant [{}]", tenant);
        return tenant;
    }

    private void setCurrentTenant(Tenant tenant) {
        LOGGER.debug("Setting current tenant to [{}]", tenant);
        currentTenantHolder.set(tenant);
    }

    @Override
    public <Result, Exc extends Throwable> List<TenantResult<Result>> executeForEachTenant(CallableResultAndException<Result, Exc> callable)
            throws Exc {
        Set<Tenant> tenants = getProvisionedTenants();
        LOGGER.debug("Will execute code for [{}] tenants [{}]...", tenants.size(), tenants);
        List<TenantResult<Result>> results = new ArrayList<>(tenants.size());

        for (Tenant tenant : tenants) {
            Result result = execute(tenant, callable);
            results.add(new TenantResultImpl<>(tenant, result));
        }

        return results;
    }

    private Set<Tenant> getProvisionedTenants() {
        Set<Tenant> tenants = tenantService.findByStatus(TenantStatus.PROVISIONED)
                                           .stream()
                                           .map(TenantImpl::createFromEntity)
                                           .collect(Collectors.toSet());
        Set<Tenant> allTenants = new HashSet<>(tenants);
        allTenants.add(TenantImpl.getDefaultTenant());
        allTenants.addAll(tenants);
        return allTenants;
    }

}
