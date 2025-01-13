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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class TenantPropertyManager.
 */
@Component
class TenantPropertyManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantPropertyManager.class);

    /** The Constant TENANT_ID_PARAM_NAME. */
    private static final String TENANT_ID_PARAM_NAME = "tenant_id";

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The defualt tenant. */
    private final Tenant defualtTenant;

    /**
     * Instantiates a new tenant property manager.
     *
     * @param tenantContext the tenant context
     * @param defualtTenant the defualt tenant
     */
    TenantPropertyManager(TenantContext tenantContext, @DefaultTenant Tenant defualtTenant) {
        this.tenantContext = tenantContext;
        this.defualtTenant = defualtTenant;
    }

    /**
     * Sets the current tenant.
     *
     * @param message the new current tenant
     * @throws JMSException the JMS exception
     */
    void setCurrentTenant(Message message) throws JMSException {
        String tenantId = getCurrentTenantId();
        LOGGER.debug("Will set tenant id [{}].", tenantId);
        message.setObjectProperty(TENANT_ID_PARAM_NAME, getCurrentTenantId());
    }

    /**
     * Gets the current tenant id.
     *
     * @return the current tenant id
     */
    private String getCurrentTenantId() {
        return tenantContext.isNotInitialized() ? defualtTenant.getId()
                : tenantContext.getCurrentTenant()
                               .getId();
    }

    /**
     * Gets the current tenant id.
     *
     * @param message the message
     * @return the current tenant id
     * @throws JMSException the JMS exception
     */
    String getCurrentTenantId(Message message) throws JMSException {
        Object tenantId = message.getObjectProperty(TENANT_ID_PARAM_NAME);
        if (null == tenantId) {
            throw new IllegalArgumentException("Tenant id parameter [" + TENANT_ID_PARAM_NAME + "] cannot be null in message: " + message);
        }
        if (tenantId instanceof String tenantIdString) {
            return tenantIdString;
        } else {
            throw new IllegalArgumentException(
                    "Invalid tenant id param [{" + tenantId + "}] with name [" + TENANT_ID_PARAM_NAME + "] in message: " + message);
        }
    }
}
