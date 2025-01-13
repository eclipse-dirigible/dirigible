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

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.stereotype.Component;

/**
 * A factory for creating AsynchronousMessageListener objects.
 */
@Component
class AsynchronousMessageListenerFactory {

    /** The tenant property manager. */
    private final TenantPropertyManager tenantPropertyManager;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new asynchronous message listener factory.
     *
     * @param tenantPropertyManager the tenant property manager
     * @param tenantContext the tenant context
     */
    AsynchronousMessageListenerFactory(TenantPropertyManager tenantPropertyManager, TenantContext tenantContext) {
        this.tenantPropertyManager = tenantPropertyManager;
        this.tenantContext = tenantContext;
    }

    /**
     * Creates the.
     *
     * @param listenerDescriptor the listener descriptor
     * @return the asynchronous message listener
     */
    AsynchronousMessageListener create(ListenerDescriptor listenerDescriptor) {
        return new AsynchronousMessageListener(listenerDescriptor, tenantPropertyManager, tenantContext);
    }
}
