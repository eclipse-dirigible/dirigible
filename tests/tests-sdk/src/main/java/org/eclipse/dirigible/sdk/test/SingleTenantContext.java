/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.test;

import java.util.List;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantResult;

/**
 * The slice runs as the platform's default tenant, outside any tenant-scoped call - what a write
 * made on a single-tenant instance sees.
 */
final class SingleTenantContext implements TenantContext {

    /** The platform's default tenant, as a request with no tenant subdomain resolves it. */
    static final Tenant DEFAULT_TENANT = new DefaultTenant();

    @Override
    public boolean isNotInitialized() {
        return true;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public Tenant getCurrentTenant() {
        return DEFAULT_TENANT;
    }

    @Override
    public <Result, Exc extends Throwable> Result execute(Tenant tenant, CallableResultAndException<Result, Exc> callable) throws Exc {
        return callable.call();
    }

    @Override
    public <Result, Exc extends Throwable> Result execute(String tenantId, CallableResultAndException<Result, Exc> callable) throws Exc {
        return callable.call();
    }

    @Override
    public <Result, Exc extends Throwable> List<TenantResult<Result>> executeForEachTenant(CallableResultAndException<Result, Exc> callable)
            throws Exc {
        Result result = callable.call();
        return List.of(new TenantResult<>() {

            @Override
            public Tenant getTenant() {
                return DEFAULT_TENANT;
            }

            @Override
            public Result getResult() {
                return result;
            }
        });
    }

    private static final class DefaultTenant implements Tenant {

        private static final long serialVersionUID = 1L;

        @Override
        public String getId() {
            return "default-tenant";
        }

        @Override
        public boolean isDefault() {
            return true;
        }

        @Override
        public String getName() {
            return "Default";
        }

        @Override
        public String getSubdomain() {
            return "default";
        }
    }
}
