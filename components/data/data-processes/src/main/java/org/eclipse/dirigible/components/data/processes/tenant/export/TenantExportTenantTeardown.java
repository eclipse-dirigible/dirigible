/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.processes.tenant.export;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantTeardownParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deprovisioning: deletes the tenant's export rows. The archives go with the tenant's CMS tree,
 * which the CMS's own participant removes.
 *
 * <p>
 * Present whether or not the export is enabled: rows written while it was on must not outlive their
 * tenant after it was turned off.
 */
@Component
class TenantExportTenantTeardown implements TenantTeardownParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExportTenantTeardown.class);

    /** The order of this participant: a row describing the tenant. */
    static final int ORDER = 360;

    private final TenantExportRepository exports;

    TenantExportTenantTeardown(TenantExportRepository exports) {
        this.exports = exports;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void teardown(String tenantId) {
        long deleted = exports.deleteByTenantId(tenantId);
        LOGGER.info("Deleted [{}] export(s) of tenant [{}].", deleted, LogSanitizer.sanitize(tenantId));
    }
}
