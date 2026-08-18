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

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The single owner of the physical JMS destination name — and therefore the home of the contract
 * that every messaging call site shares.
 *
 * <p>
 * A destination name that a project authors — a {@code .listener} artefact's name, a client-Java
 * {@code MessageHandler.destination()}, the argument to {@code Producer.sendToTopic} — is
 * <em>logical</em>. Its physical name on the broker is always tenant-scoped:
 * {@code <tenantId>###<name>} outside the default tenant, and the bare name inside it, so a
 * single-tenant deployment never sees a prefix at all. Three consequences, which only work if all
 * sides hold them together:
 * <ul>
 * <li>a <b>producer</b> resolves the name from the tenant that is current at send time —
 * {@link MessageProducer};</li>
 * <li>a <b>synchronous consumer</b> resolves it the same way, so it can only ever draw its own
 * tenant's messages — {@link MessageConsumer};</li>
 * <li>a <b>subscriber</b> is not bound to a single tenant, so it opens <b>one subscription per
 * provisioned tenant</b>. The {@code .listener} path gets that for free from its multitenant
 * synchronizer ({@link ListenerCreator} is called once per tenant); the client-Java
 * {@code MessageHandler} / {@code @Listener} path, whose class loading happens off any tenant
 * thread, fans out explicitly ({@code ListenerClassConsumer} in {@code engine-java}).</li>
 * </ul>
 *
 * <p>
 * The {@code tenant_id} message property ({@link TenantPropertyManager}) remains the authority for
 * <em>dispatch</em>: it is what re-establishes the tenant context on the broker thread, and it also
 * covers a message that was published with no tenant scope at all. It does not replace the prefix —
 * the prefix is what keeps one tenant's messages out of another tenant's consumers in the first
 * place, which the property alone cannot do for a queue (competing consumers) or for a synchronous
 * receive.
 */
@Component
public class DestinationNameManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DestinationNameManager.class);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new destination name manager.
     *
     * @param tenantContext the tenant context
     */
    DestinationNameManager(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * The physical destination name for a logical one, scoped to the tenant that is current on this
     * thread. Outside any tenant context (a platform thread that never entered one) the name is
     * returned unchanged.
     *
     * @param destinationName the logical destination name
     * @return the physical destination name to create on the broker
     */
    public String toTenantName(String destinationName) {
        if (tenantContext.isNotInitialized()) {
            LOGGER.debug("Tenant context is NOT initialized. Will return destination name as it is. Destination name [{}]",
                    destinationName);
            return destinationName;
        }
        Tenant currentTenant = tenantContext.getCurrentTenant();
        return currentTenant.isDefault() ? destinationName : currentTenant.getId() + "###" + destinationName;
    }
}
