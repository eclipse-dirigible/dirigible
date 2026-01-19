/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms.internal;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.cms.CmsProvider;
import org.eclipse.dirigible.components.engine.cms.CmsProviderFactory;
import org.eclipse.dirigible.components.engine.cms.internal.provider.CmsProviderInternal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating CmsProviderInternal objects.
 */
@Component("cms-provider-internal")
class CmsProviderInternalFactory implements CmsProviderFactory, DisposableBean {

    /** The Constant PROVIDERS. */
    private static final Map<String, CmsProvider> PROVIDERS = new HashMap<String, CmsProvider>();

    private static final Logger LOGGER = LoggerFactory.getLogger(CmsProviderInternalFactory.class);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /**
     * Instantiates a new cms provider internal factory.
     *
     * @param tenantContext the tenant context
     * @param defaultTenant the default tenant
     */
    CmsProviderInternalFactory(TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    /**
     * Creates the.
     *
     * @return the cms provider
     */
    @Override
    public CmsProvider create() {
        String rootFolder = DirigibleConfig.CMS_INTERNAL_ROOT_FOLDER.getStringValue() + getTenantFolder();
        Path path = Paths.get(rootFolder);
        boolean absolutePath = path.isAbsolute();
        if (PROVIDERS.containsKey(rootFolder)) {
            return PROVIDERS.get(rootFolder);
        }
        CmsProviderInternal cmsProviderInternal = new CmsProviderInternal(rootFolder, absolutePath);
        PROVIDERS.put(rootFolder, cmsProviderInternal);
        return cmsProviderInternal;
    }

    /**
     * Gets the tenant folder.
     *
     * @return the tenant folder
     */
    private String getTenantFolder() {
        String tenantId = tenantContext.isNotInitialized() ? defaultTenant.getId()
                : tenantContext.getCurrentTenant()
                               .getId();
        return File.separator + tenantId;
    }

    @Override
    public void destroy() {
        LOGGER.info("Deleting providers...");
        PROVIDERS.clear();
    }
}
