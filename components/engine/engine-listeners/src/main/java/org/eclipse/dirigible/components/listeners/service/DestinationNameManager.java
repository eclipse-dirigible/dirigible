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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 *
 * <p>
 * <b>The one exception: a global destination.</b> Tenant scoping is right for a destination that
 * belongs to the application, and wrong for one that <em>is a contract with someone else</em> — an
 * integration queue two products agreed on. A name that opens with the {@value #GLOBAL_MARKER}
 * marker declares exactly that: it resolves to the bare name behind the marker, in every tenant and
 * on every call site, so a consumer in another deployment — which knows nothing of this one's
 * tenants — can bind to the name it was given. Convention for such a name is
 * {@code global:<vendor>.<purpose>}, e.g. {@code global:codbex.orders}, because a global
 * destination is by definition shared with something the platform cannot see, and only a namespaced
 * name keeps two products off each other's queues.
 *
 * <p>
 * The trade is deliberate: the destination no longer says which tenant a message belongs to, so a
 * message published to a global destination is stamped with the <em>default</em> tenant
 * ({@link TenantPropertyManager}) and the business tenant, if it matters, has to travel in the
 * payload. Publishing one from a non-default tenant is legitimate but is a contract decision, so it
 * is logged at WARN — once per tenant and destination, since a producer may run hot.
 */
@Component
public class DestinationNameManager {

    /** The Constant GLOBAL_MARKER. */
    public static final String GLOBAL_MARKER = "global:";

    /** The Constant TENANT_SEPARATOR. */
    private static final String TENANT_SEPARATOR = "###";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DestinationNameManager.class);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** Tenant + global destination pairs already warned about. */
    private final Set<String> warnedGlobalUses = ConcurrentHashMap.newKeySet();

    /**
     * Instantiates a new destination name manager.
     *
     * @param tenantContext the tenant context
     */
    DestinationNameManager(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * Whether a logical destination name declares itself a contract with something outside this
     * deployment, and is therefore never tenant-scoped.
     *
     * @param destinationName the logical destination name
     * @return true, if the name carries the global marker
     */
    public static boolean isGlobal(String destinationName) {
        return null != destinationName && destinationName.startsWith(GLOBAL_MARKER);
    }

    /**
     * The physical destination name for a logical one, scoped to the tenant that is current on this
     * thread. Outside any tenant context (a platform thread that never entered one) the name is
     * returned unchanged, and so is a global one, which is never scoped at all.
     *
     * @param destinationName the logical destination name
     * @return the physical destination name to create on the broker
     */
    public String toTenantName(String destinationName) {
        if (isGlobal(destinationName)) {
            return toGlobalName(destinationName);
        }
        if (tenantContext.isNotInitialized()) {
            LOGGER.debug("Tenant context is NOT initialized. Will return destination name as it is. Destination name [{}]",
                    destinationName);
            return destinationName;
        }
        Tenant currentTenant = tenantContext.getCurrentTenant();
        return currentTenant.isDefault() ? destinationName : currentTenant.getId() + TENANT_SEPARATOR + destinationName;
    }

    /**
     * Strip the marker and keep the bare name, whatever the current tenant is.
     *
     * @param destinationName the marked logical destination name
     * @return the physical destination name, shared with whoever else agreed on it
     */
    private String toGlobalName(String destinationName) {
        String globalName = destinationName.substring(GLOBAL_MARKER.length());
        if (globalName.isBlank()) {
            throw new IllegalArgumentException(
                    "Destination name [" + destinationName + "] carries the '" + GLOBAL_MARKER + "' marker but names nothing after it.");
        }
        warnOnceIfNotDefaultTenant(globalName);
        return globalName;
    }

    /**
     * Note that a tenant is reaching a destination that carries no tenant of its own. Warned once per
     * tenant and destination: the set is bounded by the destinations an application actually names,
     * while a producer on a busy queue would otherwise log per message.
     *
     * @param globalName the physical destination name
     */
    private void warnOnceIfNotDefaultTenant(String globalName) {
        if (tenantContext.isNotInitialized()) {
            return;
        }
        Tenant currentTenant = tenantContext.getCurrentTenant();
        if (currentTenant.isDefault()) {
            return;
        }
        if (warnedGlobalUses.add(currentTenant.getId() + TENANT_SEPARATOR + globalName)) {
            LOGGER.warn("Tenant [{}] uses the global destination [{}], which is shared with every other tenant and with any other "
                    + "deployment bound to that name. Messages on it are stamped with the default tenant, so a business tenant that "
                    + "matters downstream has to travel in the payload.", currentTenant.getId(), globalName);
        }
    }
}
