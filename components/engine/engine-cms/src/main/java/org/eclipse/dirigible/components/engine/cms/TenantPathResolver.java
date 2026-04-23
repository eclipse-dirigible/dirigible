/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.cms;

import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.repository.api.IRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TenantPathResolver {

    private static final Logger logger = LoggerFactory.getLogger(TenantPathResolver.class);

    private static final String PATH_SEPARATOR = "/";

    private static final String ROOT_PATH = "/";

    private final TenantContext tenantContext;

    private final Tenant defaultTenant;

    TenantPathResolver(TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    public String resolve(String path) {
        logger.debug("Resolving tenant path for [{}]", path);
        String tenantPath = determinePath(path);
        logger.debug("Path [{}] is resolved to tenant path [{}]", path, tenantPath);

        String fixedPath = removeDoubleSlash(tenantPath);
        logger.debug("Path [{}] is finally resolved to tenant path [{}]", path, fixedPath);

        return fixedPath;
    }

    private @NonNull String determinePath(String path) {
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

    private static String removeDoubleSlash(String path) {
        // Regex "/{2,}" matches 2 or more forward slashes
        return path.replaceAll(IRepository.SEPARATOR + "{2,}", IRepository.SEPARATOR);
    }

}
