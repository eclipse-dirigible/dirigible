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

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Derives how far the initialization of a tenant has got.
 *
 * <p>
 * Derived, never tracked. Activation blanks the checksums of every definition that is materialized
 * per tenant and lets the synchronizers reprocess them; a definition whose checksum is still blank
 * is one the pass has not reached yet. Both halves of that - the blanking and the reprocessing -
 * are durable rows in the system database that every node of a cluster shares, so the answer here
 * is the same from any instance and survives a restart, without a run registry that could disagree
 * with reality.
 *
 * <p>
 * Failure is read from both places it can appear. A definition that could not be parsed is recorded
 * as {@code BROKEN} with its message; an artefact that parsed but could not be materialized - a
 * table the tenant's user may not create, say - is recorded on the artefact itself as
 * {@code FAILED} or {@code FATAL} with its error. Watching only the definitions would miss exactly
 * the failure this API exists to report, because materializing into the tenant's schema is the part
 * that involves the externally created credentials.
 *
 * <p>
 * Two properties of the signal are deliberate and worth knowing. It is batch-wide: tenants
 * activated within one synchronization window share it, so each reads {@code IN_PROGRESS} until the
 * window closes, and a definition error is reported to all of them because definition errors are
 * global. And an instance that has no per-tenant artefacts at all has nothing to materialize, so it
 * answers {@code COMPLETED} at once - which is the truth for such a deployment.
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

    /**
     * Instantiates a new tenant initialization status calculator.
     *
     * @param multitenantSynchronizers the multitenant synchronizers
     * @param definitionService the definition service
     */
    TenantInitializationStatusCalculator(MultitenantSynchronizers multitenantSynchronizers, DefinitionService definitionService) {
        this.multitenantSynchronizers = multitenantSynchronizers;
        this.definitionService = definitionService;
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

        List<Definition> definitions = definitionService.findByTypes(multitenantSynchronizers.getArtefactTypes());
        if (definitions.stream()
                       .anyMatch(TenantInitializationStatusCalculator::isAwaitingProcessing)) {
            return TenantInitializationState.of(InitializationStatus.IN_PROGRESS);
        }

        List<String> errors = collectErrors(definitions);
        if (!errors.isEmpty()) {
            LOGGER.debug("Initialization of tenant [{}] is failed with [{}] error(s).", tenant.getId(), errors.size());
            return new TenantInitializationState(InitializationStatus.FAILED, describe(errors));
        }
        return TenantInitializationState.of(InitializationStatus.COMPLETED);
    }

    /**
     * Whether the synchronizers still owe this definition a pass.
     *
     * <p>
     * A blank checksum is the mark the activation leaves; the pass replaces it with the real one as
     * soon as it collects the definition. A deleted definition never gets one back - its source file is
     * gone - so it is excluded, or a single removed artefact would leave every later activation
     * reporting progress forever.
     *
     * @param definition the definition
     * @return true, if the definition has not been reprocessed yet
     */
    private static boolean isAwaitingProcessing(Definition definition) {
        if (DefinitionState.DELETED == definition.getState()) {
            return false;
        }
        String checksum = definition.getChecksum();
        return checksum == null || checksum.isBlank();
    }

    /**
     * Everything that went wrong, from the definitions and from the artefacts they produced.
     *
     * @param definitions the multitenant definitions
     * @return the failures
     */
    private List<String> collectErrors(List<Definition> definitions) {
        List<String> errors = new ArrayList<>();
        definitions.stream()
                   .filter(definition -> DefinitionState.BROKEN == definition.getState())
                   .forEach(definition -> errors.add(
                           definition.getType() + " [" + definition.getLocation() + "]: " + definition.getMessage()));

        for (Synchronizer<?, ?> synchronizer : multitenantSynchronizers.getSynchronizers()) {
            for (Artefact artefact : readArtefacts(synchronizer)) {
                if (ArtefactLifecycle.FAILED == artefact.getLifecycle() || ArtefactLifecycle.FATAL == artefact.getLifecycle()) {
                    errors.add(artefact.getType() + " [" + artefact.getLocation() + "]: " + artefact.getError());
                }
            }
        }
        return errors;
    }

    /**
     * Reads one artefact type. A type whose table cannot be read - an engine switched off on this
     * instance, say - must not turn the whole answer into an error of its own.
     *
     * @param synchronizer the synchronizer
     * @return its artefacts, empty when they cannot be read
     */
    private List<? extends Artefact> readArtefacts(Synchronizer<?, ?> synchronizer) {
        try {
            return synchronizer.getService()
                               .getAll();
        } catch (RuntimeException ex) {
            LOGGER.warn("Failed to read the artefacts of [{}] while calculating the initialization status.", synchronizer, ex);
            return List.of();
        }
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
