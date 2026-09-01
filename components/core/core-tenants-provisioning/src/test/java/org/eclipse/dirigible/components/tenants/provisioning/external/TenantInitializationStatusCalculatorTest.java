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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantSynchronizers;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The whole derivation matrix. Every case here is one a poller acts on, so getting any of them
 * wrong either strands a provisioning process or lets it declare success over a tenant that has
 * nothing in it.
 */
class TenantInitializationStatusCalculatorTest {

    private static final Set<String> MULTITENANT_TYPES = Set.of("table", "csvim");

    private final MultitenantSynchronizers multitenantSynchronizers = mock(MultitenantSynchronizers.class);
    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final TenantInitializationStatusCalculator calculator =
            new TenantInitializationStatusCalculator(multitenantSynchronizers, definitionService);

    @BeforeEach
    void wireDefaults() {
        when(multitenantSynchronizers.getArtefactTypes()).thenReturn(MULTITENANT_TYPES);
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of());
        when(definitionService.findByTypes(any())).thenReturn(List.of());
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
     * The mark the activation leaves. Reading anything else here would let a poller skip the wait
     * entirely and report a tenant ready before a single table of it existed.
     */
    @Test
    void aBlankedDefinitionMeansTheWorkIsStillAhead() {
        when(definitionService.findByTypes(any())).thenReturn(List.of(definition("table", "", DefinitionState.PARSED)));

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aDefinitionWithoutAnyChecksumMeansTheWorkIsStillAhead() {
        when(definitionService.findByTypes(any())).thenReturn(List.of(definition("table", null, DefinitionState.NEW)));

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void reprocessedDefinitionsMeanTheInitializationIsDone() {
        when(definitionService.findByTypes(any())).thenReturn(
                List.of(definition("table", "CHECKSUM", DefinitionState.PARSED), definition("csvim", "CHECKSUM", DefinitionState.PARSED)));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.COMPLETED, state.status());
        assertNull(state.error());
    }

    /**
     * A definition whose source file is gone never gets its checksum back, so counting it as pending
     * would leave every activation of the instance reporting progress forever.
     */
    @Test
    void aDeletedDefinitionDoesNotHoldTheInitializationOpen() {
        when(definitionService.findByTypes(any())).thenReturn(
                List.of(definition("table", "", DefinitionState.DELETED), definition("csvim", "CHECKSUM", DefinitionState.PARSED)));

        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void anInstanceWithNothingToMaterializeIsImmediatelyComplete() {
        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aDefinitionThatCannotBeParsedIsAFailure() {
        Definition broken = definition("table", "CHECKSUM", DefinitionState.BROKEN);
        broken.setMessage("Unexpected token at line 3");
        when(definitionService.findByTypes(any())).thenReturn(List.of(broken));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.FAILED, state.status());
        assertTrue(state.error()
                        .contains("Unexpected token at line 3"),
                state.error());
        assertTrue(state.error()
                        .contains("/acme/customer.table"),
                state.error());
    }

    /**
     * The failure this API exists to report: the definition parsed, but materializing it into the
     * tenant's schema - with the externally created credentials - did not work. It is recorded on the
     * artefact, not on the definition, so watching definitions alone would call this a success.
     */
    @Test
    void anArtefactThatCouldNotBeMaterializedIsAFailure() {
        when(definitionService.findByTypes(any())).thenReturn(List.of(definition("table", "CHECKSUM", DefinitionState.PARSED)));
        Synchronizer<?, ?> synchronizer = synchronizerReturning(
                artefact("table", "/acme/customer.table", ArtefactLifecycle.FAILED, "Insufficient privilege to create table"));
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of(synchronizer));

        TenantInitializationState state = calculate(TenantStatus.PROVISIONED);

        assertEquals(InitializationStatus.FAILED, state.status());
        assertTrue(state.error()
                        .contains("Insufficient privilege to create table"),
                state.error());
    }

    @Test
    void aFatalArtefactIsAFailureToo() {
        Synchronizer<?, ?> synchronizer =
                synchronizerReturning(artefact("table", "/acme/customer.table", ArtefactLifecycle.FATAL, "Dependency cycle"));
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of(synchronizer));

        assertEquals(InitializationStatus.FAILED, calculate(TenantStatus.PROVISIONED).status());
    }

    @Test
    void aSuccessfullyCreatedArtefactIsNotAFailure() {
        Synchronizer<?, ?> synchronizer = synchronizerReturning(artefact("table", "/acme/customer.table", ArtefactLifecycle.CREATED, null));
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of(synchronizer));

        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    /** Work still ahead outranks failures already recorded - the pass may yet repair them. */
    @Test
    void pendingWorkOutranksAnAlreadyRecordedFailure() {
        when(definitionService.findByTypes(any())).thenReturn(List.of(definition("table", "", DefinitionState.PARSED)));
        Synchronizer<?, ?> synchronizer =
                synchronizerReturning(artefact("table", "/acme/customer.table", ArtefactLifecycle.FAILED, "Insufficient privilege"));
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of(synchronizer));

        assertEquals(InitializationStatus.IN_PROGRESS, calculate(TenantStatus.PROVISIONED).status());
    }

    /** An artefact table that cannot be read is not an initialization failure of its own. */
    @Test
    void anUnreadableArtefactTypeIsSkipped() {
        Synchronizer<?, ?> broken = mock(Synchronizer.class);
        ArtefactService<?, ?> service = mock(ArtefactService.class);
        when(service.getAll()).thenThrow(new IllegalStateException("Table DIRIGIBLE_TABLES does not exist"));
        doReturnService(broken, service);
        when(multitenantSynchronizers.getSynchronizers()).thenReturn(List.of(broken));

        assertEquals(InitializationStatus.COMPLETED, calculate(TenantStatus.PROVISIONED).status());
    }

    private TenantInitializationState calculate(TenantStatus status) {
        Tenant tenant = new Tenant("-", "Acme Ltd", "", "acme", status);
        tenant.setId("acme");
        return calculator.calculate(tenant);
    }

    private static Definition definition(String type, String checksum, DefinitionState state) {
        Definition definition = new Definition("/acme/customer." + type, "customer", type, new byte[0]);
        definition.setChecksum(checksum);
        definition.setState(state);
        return definition;
    }

    private static Artefact artefact(String type, String location, ArtefactLifecycle lifecycle, String error) {
        Artefact artefact = new Artefact(location, "customer", type, "", null) {};
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        return artefact;
    }

    /**
     * Always call this BEFORE opening a {@code when(...)} on another mock: it stubs mocks of its own,
     * and Mockito cannot nest that inside an unfinished stubbing.
     *
     * @param artefact the artefact the synchronizer's service reports
     * @return the synchronizer
     */
    private static Synchronizer<?, ?> synchronizerReturning(Artefact artefact) {
        Synchronizer<?, ?> synchronizer = mock(Synchronizer.class);
        ArtefactService<?, ?> service = mock(ArtefactService.class);
        doReturnArtefacts(service, artefact);
        doReturnService(synchronizer, service);
        return synchronizer;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void doReturnArtefacts(ArtefactService<?, ?> service, Artefact artefact) {
        when(((ArtefactService) service).getAll()).thenReturn(List.of(artefact));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void doReturnService(Synchronizer<?, ?> synchronizer, ArtefactService<?, ?> service) {
        when(((Synchronizer) synchronizer).getService()).thenReturn(service);
    }
}
