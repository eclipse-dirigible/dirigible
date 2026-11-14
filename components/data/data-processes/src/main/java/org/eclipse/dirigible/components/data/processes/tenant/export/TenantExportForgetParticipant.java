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
import org.eclipse.dirigible.components.base.tenant.TenantUserForgetParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A person's erasure: the exports they started no longer say who started them. The exports
 * themselves stay - they are the tenant's.
 */
@Component
class TenantExportForgetParticipant implements TenantUserForgetParticipant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExportForgetParticipant.class);

    private final TenantExportRepository exports;

    TenantExportForgetParticipant(TenantExportRepository exports) {
        this.exports = exports;
    }

    @Override
    public void forget(String tenantId, String email) {
        int changed = exports.clearRequestedBy(tenantId, email);
        LOGGER.info("Blanked the requester of [{}] export(s) of tenant [{}].", changed, LogSanitizer.sanitize(tenantId));
    }
}
