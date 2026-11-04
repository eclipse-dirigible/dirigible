/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suspends and resumes a tenant on behalf of an external provisioner, without touching its data.
 *
 * <p>
 * <b>Suspend: the status first, then the runtime stops.</b> The tenant is saved
 * {@link TenantStatus#SUSPENDED} before anything is stopped - saving evicts the tenant resolution
 * caches, so its people and integration clients are refused from their next request on, and
 * {@code executeForEachTenant} skips it from that moment, across a restart too. Then every
 * {@link TenantSuspensionParticipant} stops its work of the tenant, lower order first.
 *
 * <p>
 * <b>Resume: the participants first, then the status.</b> Every participant restarts its work in
 * reverse order while the tenant is still suspended, then the tenant is saved
 * {@link TenantStatus#PROVISIONED} and every participant is told it is running again, which is how
 * the work that reaches tenants through the provisioned fan-out only is picked up at once.
 *
 * <p>
 * A participant that throws stops the run with a 500 naming it. The tenant is left as it was:
 * {@code SUSPENDED} in both directions - the safe side, still refused - and the repeated request
 * finishes the run, since both halves are idempotent. That is also why suspending a suspended
 * tenant and resuming a provisioned one are not refused: they run their half again.
 *
 * <p>
 * The runtime half acts on this instance only, so an application is expected to run as a single
 * instance - as for deprovisioning.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantSuspensionService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantSuspensionService.class);

    /** The statuses a caller may ask for. */
    private static final Set<TenantStatus> REQUESTABLE = Set.of(TenantStatus.SUSPENDED, TenantStatus.PROVISIONED);

    /** The tenant service. */
    private final TenantService tenantService;

    /** Renders the tenant the way every route of this API answers it. */
    private final TenantRegistrationService registrationService;

    /** Every participant, in the order they suspend. */
    private final List<TenantSuspensionParticipant> participants;

    /** The default tenant's id - the one tenant that can never be suspended. */
    private final String defaultTenantId;

    /**
     * Instantiates a new tenant suspension service.
     *
     * @param tenantService the tenant service
     * @param registrationService renders the tenant
     * @param participants every suspension participant
     * @param defaultTenant the default tenant
     */
    @Autowired
    TenantSuspensionService(TenantService tenantService, TenantRegistrationService registrationService,
            List<TenantSuspensionParticipant> participants,
            @DefaultTenant org.eclipse.dirigible.components.base.tenant.Tenant defaultTenant) {
        this(tenantService, registrationService, participants, defaultTenant.getId());
    }

    /**
     * Instantiates a new tenant suspension service with a given default tenant id - the seam the tests
     * use.
     *
     * @param tenantService the tenant service
     * @param registrationService renders the tenant
     * @param participants every suspension participant
     * @param defaultTenantId the default tenant's id
     */
    TenantSuspensionService(TenantService tenantService, TenantRegistrationService registrationService,
            List<TenantSuspensionParticipant> participants, String defaultTenantId) {
        this.tenantService = tenantService;
        this.registrationService = registrationService;
        this.participants = participants.stream()
                                        .sorted(Comparator.comparingInt(TenantSuspensionParticipant::order))
                                        .toList();
        this.defaultTenantId = defaultTenantId;
    }

    /**
     * Suspends or resumes the tenant.
     *
     * @param tenantId the tenant id
     * @param requested {@link TenantStatus#SUSPENDED} to suspend, {@link TenantStatus#PROVISIONED} to
     *        resume
     * @return the tenant as it now stands
     */
    TenantProvisioningState changeStatus(String tenantId, TenantStatus requested) {
        if (!TenantIds.isValid(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TenantIds.invalidMessage(tenantId));
        }
        if (defaultTenantId.equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The default tenant [" + tenantId + "] can never be suspended");
        }
        if (requested == null || !REQUESTABLE.contains(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status [" + requested + "]. A tenant is suspended with ["
                    + TenantStatus.SUSPENDED + "] and resumed with [" + TenantStatus.PROVISIONED + "].");
        }
        Tenant tenant = registrationService.requireTenant(tenantId);
        if (!REQUESTABLE.contains(tenant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant [" + tenantId + "] is [" + tenant.getStatus()
                    + "] - only a provisioned or a suspended tenant can be suspended or resumed");
        }
        Tenant changed = TenantStatus.SUSPENDED == requested ? suspend(tenant) : resume(tenant);
        return registrationService.toState(changed);
    }

    private Tenant suspend(Tenant tenant) {
        String tenantId = tenant.getId();
        Tenant suspended = tenant;
        if (TenantStatus.SUSPENDED != tenant.getStatus()) {
            LOGGER.info("Suspending tenant [{}] - moving it from [{}] to [{}].", LogSanitizer.sanitize(tenantId), tenant.getStatus(),
                    TenantStatus.SUSPENDED);
            tenant.setStatus(TenantStatus.SUSPENDED);
            // persisted before anything is stopped, and saving evicts the tenant caches as well
            suspended = tenantService.save(tenant);
        } else {
            LOGGER.info("Tenant [{}] is already suspended. Suspending its runtime again.", LogSanitizer.sanitize(tenantId));
        }
        for (TenantSuspensionParticipant participant : participants) {
            run(participant, tenantId, "Suspending", TenantStatus.SUSPENDED, () -> participant.suspend(tenantId));
        }
        LOGGER.info("Suspended tenant [{}].", LogSanitizer.sanitize(tenantId));
        return suspended;
    }

    private Tenant resume(Tenant tenant) {
        String tenantId = tenant.getId();
        TenantStatus before = tenant.getStatus();
        if (TenantStatus.PROVISIONED == before) {
            LOGGER.info("Tenant [{}] is already provisioned. Resuming its runtime again.", LogSanitizer.sanitize(tenantId));
        }
        for (TenantSuspensionParticipant participant : participants.reversed()) {
            run(participant, tenantId, "Resuming", before, () -> participant.resume(tenantId));
        }
        Tenant resumed = tenant;
        if (TenantStatus.PROVISIONED != tenant.getStatus()) {
            LOGGER.info("Resuming tenant [{}] - moving it from [{}] to [{}].", LogSanitizer.sanitize(tenantId), tenant.getStatus(),
                    TenantStatus.PROVISIONED);
            tenant.setStatus(TenantStatus.PROVISIONED);
            // saving evicts the tenant caches, so the tenant is resolvable at once
            resumed = tenantService.save(tenant);
        }
        for (TenantSuspensionParticipant participant : participants) {
            try {
                participant.resumed();
            } catch (RuntimeException ex) {
                // The tenant is running again; what this participant did not pick up now, its own
                // reconciliation picks up on its next pass.
                LOGGER.warn("Tenant [{}] is resumed, but [{}] could not pick it up at once: {}", LogSanitizer.sanitize(tenantId),
                        nameOf(participant), ex.getMessage(), ex);
            }
        }
        LOGGER.info("Resumed tenant [{}].", LogSanitizer.sanitize(tenantId));
        return resumed;
    }

    /**
     * Runs one participant's half, turning its failure into the 500 that names it.
     *
     * @param participant the participant
     * @param tenantId the tenant id
     * @param action what is being done, for the messages
     * @param status the status the tenant stays in should the participant fail
     * @param step the participant's half
     */
    private void run(TenantSuspensionParticipant participant, String tenantId, String action, TenantStatus status, Runnable step) {
        String name = nameOf(participant);
        try {
            step.run();
        } catch (RuntimeException ex) {
            LOGGER.error("{} tenant [{}] stopped at [{}]. The tenant stays [{}]; repeating the call finishes it.", action,
                    LogSanitizer.sanitize(tenantId), name, status, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, action + " tenant [" + tenantId + "] stopped at [" + name
                    + "]: " + ex.getMessage() + " - the tenant stays " + status + ", and repeating the call finishes it", ex);
        }
        LOGGER.debug("{} tenant [{}]: [{}] done.", action, LogSanitizer.sanitize(tenantId), name);
    }

    private static String nameOf(TenantSuspensionParticipant participant) {
        return ClassUtils.getUserClass(participant)
                         .getSimpleName();
    }
}
