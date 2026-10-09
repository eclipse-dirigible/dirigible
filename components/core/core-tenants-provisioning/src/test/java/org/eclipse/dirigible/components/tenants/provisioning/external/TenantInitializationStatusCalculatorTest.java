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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactOutcome;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The whole derivation matrix. Every case here is one a poller acts on, so getting any of them
 * wrong either strands a provisioning process or lets it declare success over a tenant that has
 * nothing in it - and every answer is about the tenant asked for, never about another one (#7776).
 */
class TenantInitializationStatusCalculatorTest {

    private static final Set<String> MULTITENANT_TYPES = Set.of("table", "csvim");

    private final MultitenantSynchronizers multitenantSynchronizers = mock(MultitenantSynchronizers.class);
    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final TenantArtefactLedgerService ledger = mock(TenantArtefactLedgerService.class);
    private final TenantInitializationService initializationService = mock(TenantInitializationService.class);
    private final TenantInitializationStatusCalculator calculator =
            new TenantInitializationStatusCalculator(multitenantSynchronizers, definitionService, ledger, initializationService);

    @BeforeEach
    void wireDefaults() {
        when(multitenantSynchronizers.getArtefactTypes()).thenReturn(MULTITENANT_TYPES);
        when(definitionService.findByTypes(any())).thenReturn(List.of());
        when(ledger.findFailures(anyString())).thenReturn(List.of());
        when(initializationService.find(anyString())).thenReturn(Optional.empty());
    }

    /** Registered, maybe with a data source, but never activated. */
    @Test
    void aTenantThatWasNeverActivatedHasNotStarted() {
        assertEquals(InitializationStatus.NOT_STARTED, calculate(TenantStatus.PENDING_ACTIVATION).status());
    }

    @Test
    void aTenantOfTheBuiltInFlowThatIsNotProvisionedYetHasNotStarted() {
        assertEquals(InitializationStatus.NOT_STARTED, calculate(TenantStatus.INITIAL).status());
    }

    /**
     * The record the activation opens before it answers. Reading anything else here would let a poller
     * skip the wait entirely and report a tenant ready before a single table of it existed.
     */
    @Test
    void aRequestedInitializationIsInProgress() {
        initialization(requested());

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aRunningInitializationIsInProgress() {
        TenantInitialization running = requested();
        running.start(Instant.now());
        initialization(running);

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aFinishedInitializationWithoutFailuresIsCompleted() {
        initialization(finished(null));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.COMPLETED, state.status());
        assertNull(state.error());
    }

    /** A tenant of the built-in provisioner, or one activated before records were kept. */
    @Test
    void aProvisionedTenantWithoutAnInitializationIsCompleted() {
        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aDefinitionThatCannotBeParsedIsAFailure() {
        Definition broken = definition("table", DefinitionState.BROKEN);
        broken.setMessage("Unexpected token at line 3");
        when(definitionService.findByTypes(MULTITENANT_TYPES)).thenReturn(List.of(broken));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.FAILED, state.status());
        assertTrue(state.error()
                        .contains("Unexpected token at line 3"),
                state.error());
        assertTrue(state.error()
                        .contains("/acme/customer.table"),
                state.error());
    }

    @Test
    void aParsedDefinitionIsNotAFailure() {
        when(definitionService.findByTypes(MULTITENANT_TYPES)).thenReturn(List.of(definition("table", DefinitionState.PARSED)));

        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    /**
     * The failure this API exists to report: the definition parsed, but materializing it into the
     * tenant's schema - with the externally created credentials - did not work. It is recorded per
     * tenant, so watching definitions alone would call this a success.
     */
    @Test
    void anArtefactThatCouldNotBeMaterializedInTheTenantIsAFailure() {
        TenantArtefactOutcome failure = outcome(ArtefactLifecycle.FAILED, "Insufficient privilege to create table");
        when(ledger.findFailures("acme")).thenReturn(List.of(failure));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.FAILED, state.status());
        assertTrue(state.error()
                        .contains("Insufficient privilege to create table"),
                state.error());
        assertTrue(state.error()
                        .contains("table [/acme/customer.table]"),
                state.error());
    }

    /** Another tenant's failure is that tenant's - it never makes this one read failed (#7776). */
    @Test
    void anotherTenantsFailureIsNotThisTenantsFailure() {
        TenantArtefactOutcome failure = outcome(ArtefactLifecycle.FAILED, "Insufficient privilege to create table");
        when(ledger.findFailures("globex")).thenReturn(List.of(failure));

        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    /** The run itself failed - it could not even start, say - and left its error on the record. */
    @Test
    void aRunThatFailedIsAFailure() {
        initialization(finished("The initialization of tenants [acme] could not start within [300000] ms"));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.FAILED, state.status());
        assertTrue(state.error()
                        .contains("could not start"),
                state.error());
    }

    /** Work still ahead outranks failures already recorded - the run may yet repair them. */
    @Test
    void pendingWorkOutranksAnAlreadyRecordedFailure() {
        initialization(requested());
        TenantArtefactOutcome failure = outcome(ArtefactLifecycle.FAILED, "Insufficient privilege");
        when(ledger.findFailures("acme")).thenReturn(List.of(failure));

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    private TenantInitializationState calculate(TenantStatus status) {
        Tenant tenant = new Tenant("-", "Acme Ltd", "", "acme", status);
        tenant.setId("acme");
        return calculator.calculate(tenant);
    }

    private void initialization(TenantInitialization initialization) {
        when(initializationService.find("acme")).thenReturn(Optional.of(initialization));
    }

    private static TenantInitialization requested() {
        TenantInitialization initialization = new TenantInitialization("acme");
        initialization.request(Instant.now());
        return initialization;
    }

    private static TenantInitialization finished(String failure) {
        TenantInitialization initialization = requested();
        initialization.start(Instant.now());
        initialization.finish(Instant.now(), failure);
        return initialization;
    }

    private static Definition definition(String type, DefinitionState state) {
        Definition definition = new Definition("/acme/customer." + type, "customer", type, new byte[0]);
        definition.setChecksum("CHECKSUM");
        definition.setState(state);
        return definition;
    }

    /**
     * Always call this BEFORE opening a {@code when(...)} on another mock: it stubs a mock of its own,
     * and Mockito cannot nest that inside an unfinished stubbing.
     */
    private static TenantArtefactOutcome outcome(ArtefactLifecycle lifecycle, String error) {
        TenantArtefactOutcome outcome = mock(TenantArtefactOutcome.class);
        when(outcome.getArtefactType()).thenReturn("table");
        when(outcome.getArtefactLocation()).thenReturn("/acme/customer.table");
        when(outcome.getLifecycle()).thenReturn(lifecycle);
        when(outcome.getError()).thenReturn(error);
        return outcome;
    }
}
