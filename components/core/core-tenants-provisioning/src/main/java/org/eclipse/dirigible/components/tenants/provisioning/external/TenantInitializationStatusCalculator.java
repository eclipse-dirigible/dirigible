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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Derives how far the initialization of a tenant has got - for that tenant only.
 *
 * <p>
 * Every part of the answer is a row of the system database that every node of a cluster shares, so
 * it is the same from any instance and survives a restart: the tenant's
 * {@link TenantInitialization} says whether its initialization is still ahead or running, and the
 * per-tenant artefact ledger says what each artefact came to in this tenant. Another tenant's
 * failure is never reported here, and a later success elsewhere never hides one of this tenant's
 * (#7776).
 *
 * <p>
 * Failure is read from three places. An artefact that parsed but could not be materialized in the
 * tenant - a table the tenant's user may not create, say - is a failure in the ledger. A definition
 * that could not be parsed is {@code BROKEN}; that is a property of the definition, not of a
 * tenant, so it is reported to every tenant. And a run that failed as a whole - it could not start
 * in time, say - leaves its error on the tenant's initialization.
 *
 * <p>
 * A provisioned tenant without an initialization record - one provisioned by the built-in
 * provisioner, or activated before records were kept - has nothing pending, so it is answered from
 * the ledger and the definitions alone.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantInitializationStatusCalculator {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantInitializationStatusCalculator.class);

    /** How many failures are quoted before the detail is truncated. */
    private static final int MAX_REPORTED_ERRORS = 20;

    /** The multitenant synchronizers. */
    private final MultitenantSynchronizers multitenantSynchronizers;

    /** The definition service. */
    private final DefinitionService definitionService;

    /** The outcome of each per-tenant artefact, per tenant. */
    private final TenantArtefactLedgerService ledger;

    /** The initialization of each activated tenant. */
    private final TenantInitializationService initializationService;

    /**
     * Instantiates a new tenant initialization status calculator.
     *
     * @param multitenantSynchronizers the multitenant synchronizers
     * @param definitionService the definition service
     * @param ledger the outcome of each per-tenant artefact, per tenant
     * @param initializationService the initialization of each activated tenant
     */
    TenantInitializationStatusCalculator(MultitenantSynchronizers multitenantSynchronizers, DefinitionService definitionService,
            TenantArtefactLedgerService ledger, TenantInitializationService initializationService) {
        this.multitenantSynchronizers = multitenantSynchronizers;
        this.definitionService = definitionService;
        this.ledger = ledger;
        this.initializationService = initializationService;
    }

    /**
     * Calculate the initialization state of a tenant.
     *
     * @param tenant the tenant
     * @return the state
     */
    TenantInitializationState calculate(Tenant tenant) {
        if (TenantStatus.PROVISIONED != tenant.getStatus()) {
            return TenantInitializationState.of(InitializationStatus.NOT_STARTED);
        }

        Optional<TenantInitialization> initialization = initializationService.find(tenant.getId());
        if (initialization.map(TenantInitialization::isOpen)
                          .orElse(false)) {
            return TenantInitializationState.of(InitializationStatus.IN_PROGRESS);
        }

        List<String> errors = collectErrors(tenant.getId(), initialization);
        if (!errors.isEmpty()) {
            LOGGER.debug("Initialization of tenant [{}] is failed with [{}] error(s).", tenant.getId(), errors.size());
            return new TenantInitializationState(InitializationStatus.FAILED, describe(errors));
        }
        return TenantInitializationState.of(InitializationStatus.COMPLETED);
    }

    /**
     * Everything that went wrong for the tenant: the run, the definitions, and the artefacts.
     *
     * @param tenantId the tenant id
     * @param initialization the tenant's initialization, if it has one
     * @return the failures
     */
    private List<String> collectErrors(String tenantId, Optional<TenantInitialization> initialization) {
        List<String> errors = new ArrayList<>();
        initialization.map(TenantInitialization::getError)
                      .filter(error -> !error.isBlank())
                      .ifPresent(errors::add);
        definitionService.findByTypes(multitenantSynchronizers.getArtefactTypes())
                         .stream()
                         .filter(definition -> DefinitionState.BROKEN == definition.getState())
                         .forEach(definition -> errors.add(
                                 definition.getType() + " [" + definition.getLocation() + "]: " + definition.getMessage()));
        ledger.findFailures(tenantId)
              .forEach(
                      outcome -> errors.add(outcome.getArtefactType() + " [" + outcome.getArtefactLocation() + "]: " + outcome.getError()));
        return errors;
    }

    /**
     * Describe.
     *
     * @param errors the errors
     * @return a detail a caller can act on, bounded in size
     */
    private static String describe(List<String> errors) {
        String detail = String.join("; ", errors.subList(0, Math.min(errors.size(), MAX_REPORTED_ERRORS)));
        return errors.size() > MAX_REPORTED_ERRORS ? detail + "; and " + (errors.size() - MAX_REPORTED_ERRORS) + " more" : detail;
    }
}
