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

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.tenants.tenant.TenantEnteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Records when a person enters a tenant - the one column of the replica this application writes. It
 * stamps an existing, not removed row only: the replica's rows are the provisioning system's to
 * create, so a person it has not reported yet has their next sign-in recorded instead.
 *
 * <p>
 * Bookkeeping must never break a sign-in, so it runs in a transaction of its own inside a
 * try/catch, and a failure is a warning.
 */
@Component
@Conditional(TenantUsersEnabledCondition.class)
class TenantUserSignInListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantUserSignInListener.class);

    /** The replica. */
    private final TenantUserRepository users;

    /** A new transaction, whatever the sign-in runs in. */
    private final TransactionTemplate transactions;

    /**
     * Instantiates the listener.
     *
     * @param users the replica
     * @param transactionManager the SystemDB transaction manager
     */
    TenantUserSignInListener(TenantUserRepository users, @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {
        this.users = users;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Stamps the sign-in on the person's row.
     *
     * @param event the event
     */
    @EventListener
    void onTenantEntered(TenantEnteredEvent event) {
        try {
            String email = emailOf(event);
            if (email == null) {
                return;
            }
            transactions.executeWithoutResult(status -> users.findByTenantIdAndEmail(event.tenantId(), email)
                                                             .filter(user -> user.getStatus() != TenantUserStatus.REMOVED)
                                                             .ifPresent(user -> user.setLastSignInAt(event.at())));
        } catch (RuntimeException e) {
            LOGGER.warn("Could not record that [{}] entered tenant [{}]", LogSanitizer.sanitize(event.principal()),
                    LogSanitizer.sanitize(event.tenantId()), e);
        }
    }

    /**
     * The person's email: the email claim, else the principal name when it is an address.
     *
     * @param event the event
     * @return the normalized email, or null
     */
    static String emailOf(TenantEnteredEvent event) {
        for (String candidate : new String[] {event.email(), event.principal()}) {
            if (candidate != null) {
                String email = TenantUserRules.normalize(candidate);
                if (TenantUserRules.isEmail(email)) {
                    return email;
                }
            }
        }
        return null;
    }
}
