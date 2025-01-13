/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.s3;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class TenantPathResolved.
 */
@Component
public class TenantPathResolved {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(TenantPathResolved.class);

    /** The Constant PATH_SEPARATOR. */
    private static final String PATH_SEPARATOR = "/";

    /** The Constant ROOT_PATH. */
    private static final String ROOT_PATH = "/";

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /**
     * Instantiates a new tenant path resolved.
     *
     * @param tenantContext the tenant context
     * @param defaultTenant the default tenant
     */
    TenantPathResolved(TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    /**
     * Resolve.
     *
     * @param path the path
     * @return the string
     */
    public String resolve(String path) {
        String tenantId = tenantContext.isInitialized() ? tenantContext.getCurrentTenant()
                                                                       .getId()
                : defaultTenant.getId();
        String prefix = tenantId + PATH_SEPARATOR;
        if (ROOT_PATH.equals(path)) {
            return prefix;
        }

        if (path.startsWith(prefix)) {
            return path;
        }

        String tenantPath = prefix + (path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path);
        logger.debug("Path [{}] is resolved to [{}]", path, tenantPath);
        return tenantPath;
    }
}
