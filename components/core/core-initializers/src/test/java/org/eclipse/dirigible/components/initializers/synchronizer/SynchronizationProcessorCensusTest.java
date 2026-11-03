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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.readiness.ArtefactCensus;
import org.eclipse.dirigible.components.base.readiness.PlatformReadiness;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

/**
 * A synchronizer may settle its artefacts' lifecycles in {@code finishing()} - the client-Java
 * batch compile does - so the census and the readiness of a pass include what finishing failed
 * (#8239).
 */
class SynchronizationProcessorCensusTest {

    private static final String TYPE = "java";
    private static final String LOCATION = "/project/qa/Broken.java";
    private static final String ERROR = "cannot find symbol";

    @TempDir
    Path registry;

    /**
     * The lifecycle the synchronizer's store holds; every read of the store returns a fresh instance.
     */
    private ArtefactLifecycle stored = ArtefactLifecycle.CREATED;

    private SynchronizerCallback callback;

    private SynchronizationProcessor processor;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void bootTheProcessor() throws IOException {
        Path source = registry.resolve(LOCATION.substring(1));
        Files.createDirectories(source.getParent());
        Files.writeString(source, "package qa; public class Broken { Strin x; }");

        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());
        when(repository.getResource(anyString())).thenAnswer(invocation -> {
            IResource resource = mock(IResource.class);
            when(resource.exists()).thenReturn(Files.exists(source));
            return resource;
        });
        DefinitionService definitionService = mock(DefinitionService.class);
        when(definitionService.getAll()).thenReturn(List.of());

        Synchronizer synchronizer = mock(Synchronizer.class);
        ArtefactService service = mock(ArtefactService.class);
        when(service.getAll()).thenAnswer(invocation -> List.of(stored()));
        when(synchronizer.getService()).thenReturn(service);
        when(synchronizer.getArtefactType()).thenReturn(TYPE);
        when(synchronizer.isAccepted(TYPE)).thenReturn(true);
        doAnswer(invocation -> callback = invocation.getArgument(0)).when(synchronizer)
                                                                    .setCallback(any());
        // The batch compile of finishing() fails the source and reports it, as JavaSynchronizer does.
        doAnswer(invocation -> {
            stored = ArtefactLifecycle.FAILED;
            callback.addError("Failed client Java source with location [" + LOCATION + "], error [" + ERROR + "]");
            return null;
        }).when(synchronizer)
          .finishing();

        List<Synchronizer<?, ?>> synchronizers = new ArrayList<>();
        synchronizers.add(synchronizer);
        processor = new SynchronizationProcessor(repository, synchronizers, definitionService, mock(SynchronizationWatcher.class),
                new RegistryMutationTracker(), mock(TenantContext.class), mock(TenantArtefactLedgerService.class),
                mock(ObjectProvider.class));
        processor.prepareSynchronizers();
    }

    @Test
    void anArtefactFailedInFinishingIsCountedByTheCensusAndTheReadiness() {
        processor.processSynchronizers();

        PlatformReadiness readiness = PlatformReadiness.getInstance();
        ArtefactCensus census = readiness.getArtefacts();
        assertEquals(Map.of(TYPE, 1), census.failedByType());
        assertEquals(1, census.total());
        assertEquals(PlatformReadiness.State.READY_DEGRADED, readiness.getState());
        assertEquals(1, readiness.getFailedArtefacts());
    }

    private Artefact stored() {
        Artefact artefact = new Artefact(LOCATION, "Broken", TYPE, "", null) {};
        artefact.setLifecycle(stored);
        artefact.setError(ArtefactLifecycle.FAILED == stored ? ERROR : null);
        return artefact;
    }
}
