/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deprovisioning: deletes the tenant's integration clients - every client row and, through the
 * cascade, their role rows. From then on their tokens are refused here, whatever their lifetime.
 *
 * <p>
 * Needed because the registry's tenant id is a plain column with no foreign key to the tenant:
 * deleting the tenant alone would leave the rows behind, and a tenant registered again under the
 * same id would find them.
 */
@Component
class TenantClientsTenantTeardown implements TenantTeardownParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantClientsTenantTeardown.class);

    /** The order of this participant: a row describing the tenant, next to its users. */
    static final int ORDER = 350;

    /** The registry. */
    private final TenantClientRepository clients;

    /**
     * Instantiates a new tenant clients teardown.
     *
     * @param clients the registry
     */
    TenantClientsTenantTeardown(TenantClientRepository clients) {
        this.clients = clients;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void teardown(String tenantId) {
        long deleted = clients.deleteByTenantId(tenantId);
        LOGGER.info("Deleted [{}] integration client(s) of tenant [{}].", deleted, LogSanitizer.sanitize(tenantId));
    }
}
