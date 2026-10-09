/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.synchronizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

/**
 * Initializing a newly activated tenant creates the per-tenant artefacts in that tenant alone, and
 * leaves the shared artefact rows - and every other tenant - as they were (#7799).
 */
class SynchronizationProcessorTenantInitializationTest {

    private static final String TYPE = "table";
    private static final String LOCATION = "/orders/orders.table";
    private static final Set<String> NEW_TENANT = Set.of("t-b");

    @TempDir
    Path registry;

    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final TenantArtefactLedgerService ledger = mock(TenantArtefactLedgerService.class);
    private final RecordingSynchronizer synchronizer = new RecordingSynchronizer();
    private final List<Set<String>> scopes = new ArrayList<>();

    private SynchronizationProcessor processor;

    @BeforeEach
    void bootTheProcessor() throws Throwable {
        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());
        when(definitionService.getAll()).thenReturn(List.of());
        when(tenantContext.executeScopedTo(any(), any())).thenAnswer(invocation -> {
            scopes.add(invocation.getArgument(0));
            return invocation.<CallableResultAndException<?, ?>>getArgument(1)
                             .call();
        });

        List<Synchronizer<?, ?>> synchronizers = new ArrayList<>();
        synchronizers.add(synchronizer.mock);
        processor = new SynchronizationProcessor(repository, synchronizers, definitionService, mock(SynchronizationWatcher.class),
                new RegistryMutationTracker(), tenantContext, ledger);
        processor.prepareSynchronizers();
        // the boot pass, which an initialization waits for
        processor.processSynchronizers();
        synchronizer.completions.clear();
    }

    /**
     * For the new tenant the artefact does not exist yet, so it is created, whatever the shared row
     * says - and the fan-out it runs through is restricted to that tenant.
     */
    @Test
    void theArtefactsAreCreatedForTheNewTenantOnly() {
        Artefact table = synchronizer.store(ArtefactLifecycle.CREATED);
        definitions(definition(DefinitionState.PARSED));

        processor.initializeTenants(NEW_TENANT);

        assertEquals(List.of(NEW_TENANT), scopes);
        assertTrue(synchronizer.completions.contains(ArtefactPhase.CREATE + ":" + ArtefactLifecycle.NEW),
                synchronizer.completions.toString());
        verify(synchronizer.mock).setStatus(table, ArtefactLifecycle.CREATED, "shared error");
        verify(definitionService, never()).updateChecksums(any(), any());
    }

    /** What a previous initialization of the tenant recorded is replaced, not merged. */
    @Test
    void theTenantsPreviousOutcomesAreClearedFirst() {
        synchronizer.store(ArtefactLifecycle.CREATED);
        definitions(definition(DefinitionState.PARSED));

        processor.initializeTenants(NEW_TENANT);

        InOrder order = inOrder(ledger, tenantContext);
        order.verify(ledger)
             .clear(NEW_TENANT);
        order.verify(tenantContext)
             .executeScopedTo(eq(NEW_TENANT), any());
    }

    /**
     * A definition that is new or modified is the next full pass's - it applies it to every tenant, the
     * new one included - and one that could not be parsed has no artefact to apply.
     */
    @Test
    void onlyDefinitionsAFullPassHasProcessedAreApplied() {
        synchronizer.store(ArtefactLifecycle.CREATED);
        definitions(definition(DefinitionState.MODIFIED), definition(DefinitionState.BROKEN), definition(DefinitionState.NEW));

        processor.initializeTenants(NEW_TENANT);

        assertTrue(synchronizer.completions.isEmpty(), synchronizer.completions.toString());
    }

    /** A FATAL artefact is stripped from every pass, and from this one as well. */
    @Test
    void aFatalArtefactIsLeftOut() {
        synchronizer.store(ArtefactLifecycle.FATAL);
        definitions(definition(DefinitionState.PARSED));

        processor.initializeTenants(NEW_TENANT);

        assertTrue(synchronizer.completions.isEmpty(), synchronizer.completions.toString());
    }

    /** The slot is released and the shared row restored, whatever the initialization ran into. */
    @Test
    void aFailingInitializationStillReleasesTheSlotAndRestoresTheRow() throws Throwable {
        Artefact table = synchronizer.store(ArtefactLifecycle.CREATED);
        definitions(definition(DefinitionState.PARSED));
        doThrow(new IllegalStateException("the tenant's data source is gone")).when(tenantContext)
                                                                              .executeScopedTo(any(), any());

        assertThrows(IllegalStateException.class, () -> processor.initializeTenants(NEW_TENANT));

        assertFalse(processor.isSynchronizationRunning());
        verify(synchronizer.mock).setStatus(table, ArtefactLifecycle.CREATED, "shared error");
    }

    private void definitions(Definition... definitions) {
        when(definitionService.findByTypes(Set.of(TYPE))).thenReturn(List.of(definitions));
    }

    private static Definition definition(DefinitionState state) {
        Definition definition = new Definition(LOCATION, "orders", TYPE, new byte[0]);
        definition.setChecksum("CHECKSUM");
        definition.setState(state);
        return definition;
    }

    /**
     * A per-tenant synchronizer that records the phase and the lifecycle it was asked to complete each
     * artefact in.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class RecordingSynchronizer {

        private final Synchronizer mock = mock(Synchronizer.class);
        private final List<String> completions = new ArrayList<>();

        RecordingSynchronizer() {
            ArtefactService service = mock(ArtefactService.class);
            when(service.getAll()).thenReturn(List.of());
            when(mock.getService()).thenReturn(service);
            when(mock.multitenantExecution()).thenReturn(true);
            when(mock.getArtefactType()).thenReturn(TYPE);
            when(mock.isAccepted(TYPE)).thenReturn(true);
            when(mock.complete(any(), any())).thenAnswer(invocation -> {
                TopologyWrapper<?> wrapper = invocation.getArgument(0);
                completions.add(invocation.getArgument(1) + ":" + wrapper.getArtefact()
                                                                         .getLifecycle());
                return true;
            });
        }

        Artefact store(ArtefactLifecycle lifecycle) {
            Artefact artefact = new Artefact(LOCATION, "orders", TYPE, "", null) {};
            artefact.setLifecycle(lifecycle);
            artefact.setError("shared error");
            when(mock.retrieve(LOCATION)).thenReturn(new ArrayList<>(List.of(artefact)));
            return artefact;
        }
    }
}
