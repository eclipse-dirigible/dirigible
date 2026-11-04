/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Suspension: stops the tenant's {@code .listener} consumers - every one subscribed to a
 * destination {@code <tenantId>###<name>} - and starts them again. The destinations stay on the
 * broker, so the messages that arrive meanwhile wait there and are consumed after the resume.
 */
@Component
class ListenersTenantSuspension implements TenantSuspensionParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenersTenantSuspension.class);

    /** The order of this participant: what runs for the tenant. */
    static final int ORDER = 100;

    /** The listeners manager. */
    private final ListenersManager listenersManager;

    /**
     * Instantiates a new listeners suspension.
     *
     * @param listenersManager the listeners manager
     */
    ListenersTenantSuspension(ListenersManager listenersManager) {
        this.listenersManager = listenersManager;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void suspend(String tenantId) {
        int stopped = listenersManager.stopTenantListeners(tenantId);
        LOGGER.info("Stopped [{}] listener(s) of suspended tenant [{}].", stopped, LogSanitizer.sanitize(tenantId));
    }

    @Override
    public void resume(String tenantId) {
        int started = listenersManager.startTenantListeners(tenantId);
        LOGGER.info("Started [{}] listener(s) of resumed tenant [{}].", started, LogSanitizer.sanitize(tenantId));
    }
}
