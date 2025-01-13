/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class DestinationNameManager.
 */
@Component
class DestinationNameManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DestinationNameManager.class);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new destination name manager.
     *
     * @param tenantContext the tenant context
     */
    DestinationNameManager(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * To tenant name.
     *
     * @param destinationName the destination name
     * @return the string
     */
    String toTenantName(String destinationName) {
        if (tenantContext.isNotInitialized()) {
            LOGGER.debug("Tenant context is NOT initialized. Will return destination name as it is. Destination name [{}]",
                    destinationName);
            return destinationName;
        }
        Tenant currentTenant = tenantContext.getCurrentTenant();
        return currentTenant.isDefault() ? destinationName : currentTenant.getId() + "###" + destinationName;
    }
}
