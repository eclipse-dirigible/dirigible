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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * What nobody asks for: an export whose process will not complete it is ended as failed, and an
 * export past its expiry is deleted with its archive.
 */
@Component
@Conditional(TenantExportEnabledCondition.class)
class TenantExportHousekeeping {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExportHousekeeping.class);

    private final TenantExportService service;

    TenantExportHousekeeping(TenantExportService service) {
        this.service = service;
    }

    /**
     * Looks for exports that will not complete - often, because a person is waiting on the page.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 30_000)
    void failStuck() {
        try {
            service.failStuck();
        } catch (RuntimeException ex) {
            LOGGER.error("Looking for failed tenant exports failed", ex);
        }
    }

    /**
     * Deletes the expired exports.
     */
    @Scheduled(initialDelay = 5 * 60_000, fixedDelay = 60 * 60_000)
    void expire() {
        try {
            service.expire();
        } catch (RuntimeException ex) {
            LOGGER.error("Deleting the expired tenant exports failed", ex);
        }
    }
}
