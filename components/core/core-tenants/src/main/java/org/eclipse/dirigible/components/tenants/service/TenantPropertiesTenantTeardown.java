/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.service;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deprovisioning: deletes the tenant's
 * {@link org.eclipse.dirigible.components.tenants.domain.TenantProperties} row. Needed because its
 * tenant id is a plain column with no foreign key to the tenant: a tenant registered again under
 * the same id would otherwise find the old properties.
 */
@Component
class TenantPropertiesTenantTeardown implements TenantTeardownParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantPropertiesTenantTeardown.class);

    /**
     * The order of this participant: a row describing the tenant, after the user replica (300), whose
     * screen reads the properties, beside the initialization record (400).
     */
    static final int ORDER = 410;

    /** The properties. */
    private final TenantPropertiesService properties;

    /**
     * Instantiates a new tenant properties teardown.
     *
     * @param properties the properties
     */
    TenantPropertiesTenantTeardown(TenantPropertiesService properties) {
        this.properties = properties;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void teardown(String tenantId) {
        if (properties.delete(tenantId)) {
            LOGGER.info("Deleted the properties of tenant [{}].", LogSanitizer.sanitize(tenantId));
        }
    }
}
