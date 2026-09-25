/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import org.eclipse.dirigible.components.tenants.tenant.TenantEnteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns an invited or assigned user {@code ACTIVE} when they enter the tenant - the application's
 * own knowledge that a person has signed in, with no call to any identity provider. Bookkeeping
 * must never break a sign-in, so every failure is caught and logged.
 */
@Component
@Conditional(TenantUsersEnabledCondition.class)
class TenantUserSignInListener {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserSignInListener.class);

    /** The users. */
    private final ApplicationUserService users;

    /**
     * Instantiates the listener.
     *
     * @param users the users
     */
    TenantUserSignInListener(ApplicationUserService users) {
        this.users = users;
    }

    /**
     * Records the sign-in.
     *
     * @param event the tenant entered
     */
    @EventListener
    void onTenantEntered(TenantEnteredEvent event) {
        try {
            users.recordSignIn(event.tenantId(), event.principal(), event.email(), event.at());
        } catch (RuntimeException e) {
            LOGGER.warn("Could not record that [{}] entered tenant [{}]", event.principal(), event.tenantId(), e);
        }
    }
}
