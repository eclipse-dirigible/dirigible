/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.sources.manager;

import com.google.common.base.Objects;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.data.sources.config.SystemDataSourceName;
import org.springframework.stereotype.Component;

/**
 * The Class TenantDataSourceNameManager.
 */
@Component
public class TenantDataSourceNameManager {

    /** The tenant context. */
    private final TenantContext tenantContext;
    private final String systemDataSourceName;

    TenantDataSourceNameManager(TenantContext tenantContext, @SystemDataSourceName String systemDataSourceName) {
        this.tenantContext = tenantContext;

        this.systemDataSourceName = systemDataSourceName;
    }

    /**
     * Gets the tenant data source name.
     *
     * @param dataSourceName the data source name
     * @return the tenant data source name
     */
    public String getTenantDataSourceName(String dataSourceName) {
        if (isSystemDataSource(dataSourceName) || tenantContext.isNotInitialized()) {
            return dataSourceName;
        }
        Tenant tenant = tenantContext.getCurrentTenant();
        return createName(tenant, dataSourceName);
    }

    /**
     * Checks if is system data source.
     *
     * @param dataSourceName the data source name
     * @return true, if is system data source
     */
    private boolean isSystemDataSource(String dataSourceName) {
        return Objects.equal(dataSourceName, systemDataSourceName);
    }

    /**
     * Creates the name.
     *
     * @param tenant the tenant
     * @param dataSourceName the data source name
     * @return the string
     */
    public String createName(Tenant tenant, String dataSourceName) {
        if (isTenantDataSourceName(tenant, dataSourceName)) {
            return dataSourceName;
        }
        return tenant != null && !tenant.isDefault() ? createPrefix(tenant) + dataSourceName : dataSourceName;
    }

    /**
     * Checks if is tenant data source name.
     *
     * @param tenant the tenant
     * @param dataSourceName the data source name
     * @return true, if is tenant data source name
     */
    private boolean isTenantDataSourceName(Tenant tenant, String dataSourceName) {
        return (tenant != null) ? dataSourceName.startsWith(createPrefix(tenant)) : false;
    }

    /**
     * Creates the prefix.
     *
     * @param tenant the tenant
     * @return the string
     */
    private String createPrefix(Tenant tenant) {
        return tenant.getId() + "_";
    }

}
