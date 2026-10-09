/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;

import java.util.List;
import java.util.Set;

/**
 * The Interface TenantContext.
 */
public interface TenantContext {

    /**
     * Checks if is not initialized.
     *
     * @return true, if is not initialized
     */
    boolean isNotInitialized();

    /**
     * Checks if is initialized.
     *
     * @return true, if is initialized
     */
    boolean isInitialized();

    /**
     * Gets the current tenant.
     *
     * @return the current tenant
     */
    Tenant getCurrentTenant();

    /**
     * This method will execute callable.call() method on behalf of the specified tenant.
     *
     * @param tenant the tenant
     * @param callable the callable
     * @param <Result> result type
     * @param <Exc> exception type which is thrown by the callable
     * @return the result
     * @throws Exc exception which is thrown by the callable
     */
    <Result, Exc extends Throwable> Result execute(Tenant tenant, CallableResultAndException<Result, Exc> callable) throws Exc;

    /**
     * This method will execute callable.call() method on behalf of the specified tenant id.
     *
     * @param tenantId the tenant id
     * @param callable the callable
     * @param <Result> result type
     * @param <Exc> exception type which is thrown by the callable
     * @return the result
     * @throws TenantNotFoundException in case the provided tenant id doesn't exist
     * @throws Exc the exception which is thrown by the callable
     */
    <Result, Exc extends Throwable> Result execute(String tenantId, CallableResultAndException<Result, Exc> callable)
            throws TenantNotFoundException, Exc;

    /**
     * This method will execute callable.call() for each provisioned tenant.
     *
     * @param callable
     * @param <Result> result type
     * @param <Exc> exception type which is thrown by the callable
     * @return the results of the tenant executions
     * @throws Exc the exception which is thrown by the callable
     */
    <Result, Exc extends Throwable> List<TenantResult<Result>> executeForEachTenant(CallableResultAndException<Result, Exc> callable)
            throws Exc;

    /**
     * Executes callable.call() on the calling thread with {@link #executeForEachTenant} restricted to
     * the given tenants: every per-tenant fan-out the callable reaches, however deep, visits only the
     * provisioned tenants among them. This is how the initialization of a newly activated tenant runs
     * the per-tenant artefacts for that tenant alone, leaving the already initialized tenants
     * untouched.
     *
     * @param tenantIds the ids of the tenants the fan-out is restricted to
     * @param callable the callable
     * @param <Result> result type
     * @param <Exc> exception type which is thrown by the callable
     * @return the result
     * @throws Exc the exception which is thrown by the callable
     */
    <Result, Exc extends Throwable> Result executeScopedTo(Set<String> tenantIds, CallableResultAndException<Result, Exc> callable)
            throws Exc;

}
