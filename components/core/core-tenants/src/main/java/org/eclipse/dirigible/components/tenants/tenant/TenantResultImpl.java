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

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantResult;

/**
 * The Class TenantResultImpl.
 *
 * @param <Result> the generic type
 */
class TenantResultImpl<Result> implements TenantResult<Result> {

    /** The tenant. */
    private final Tenant tenant;

    /** The result. */
    private final Result result;

    /**
     * Instantiates a new tenant result impl.
     *
     * @param tenant the tenant
     * @param result the result
     */
    TenantResultImpl(Tenant tenant, Result result) {
        this.tenant = tenant;
        this.result = result;
    }

    /**
     * Gets the tenant.
     *
     * @return the tenant
     */
    @Override
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Gets the result.
     *
     * @return the result
     */
    @Override
    public Result getResult() {
        return result;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TenantResultImpl [tenant=" + tenant + ", result=" + result + "]";
    }

}
