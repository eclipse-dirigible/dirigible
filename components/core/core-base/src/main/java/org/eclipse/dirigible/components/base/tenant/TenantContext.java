/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.tenant;

import java.util.List;

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

}
