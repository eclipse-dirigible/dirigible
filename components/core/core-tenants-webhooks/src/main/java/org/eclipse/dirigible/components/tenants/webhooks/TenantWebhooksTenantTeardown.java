/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import java.sql.SQLException;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Deprovisioning: drops the tenant's pending deliveries and delivery log - in its own schema, so
 * before its data source goes (order 200) - and its webhook registry. From then on nothing is
 * delivered for it.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantWebhooksTenantTeardown implements TenantTeardownParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantWebhooksTenantTeardown.class);

    /** After what runs for the tenant (100), before its data source (200). */
    static final int ORDER = 150;

    private final TenantWebhookRegistryService registry;

    private final WebhookDeliveryStore store;

    private final WebhookRelay relay;

    private final TenantContext tenantContext;

    /**
     * Instantiates the teardown.
     *
     * @param registry the registry
     * @param store the deliveries
     * @param relay the relay
     * @param tenantContext the tenant context
     */
    TenantWebhooksTenantTeardown(TenantWebhookRegistryService registry, WebhookDeliveryStore store, WebhookRelay relay,
            TenantContext tenantContext) {
        this.registry = registry;
        this.store = store;
        this.relay = relay;
        this.tenantContext = tenantContext;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void teardown(String tenantId) {
        try {
            tenantContext.execute(tenantId, () -> {
                store.deleteAll();
                return null;
            });
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not delete the webhook deliveries of tenant [" + tenantId + "]", ex);
        }
        relay.forget(tenantId);
        long deleted = registry.deleteTenant(tenantId);
        LOGGER.info("Deleted [{}] webhook subscription(s) and the delivery log of tenant [{}].", deleted, LogSanitizer.sanitize(tenantId));
    }
}
