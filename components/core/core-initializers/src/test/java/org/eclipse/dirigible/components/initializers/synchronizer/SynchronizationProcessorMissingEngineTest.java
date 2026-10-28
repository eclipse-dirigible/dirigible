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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.function.Supplier;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.script.ScriptEngineUnavailableException;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

/**
 * An artefact whose handler needs a script engine that is not deployed is reported once and not
 * attempted again until its file changes (#8176), and the in-pass retry of a failed artefact stops
 * once its source is unpublished, so it no longer holds off its own removal (#8218).
 */
class SynchronizationProcessorMissingEngineTest {

    private static final String TYPE = "job";
    private static final String LOCATION = "/project/every.job";

    @TempDir
    Path registry;

    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final SynchronizationWatcher watcher = mock(SynchronizationWatcher.class);
    private final FailingSynchronizer synchronizer = new FailingSynchronizer();

    private SynchronizationProcessor processor;

    @BeforeEach
    void bootTheProcessor() throws IOException {
        Configuration.set("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT", "2");
        Configuration.set("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS", "1");
        Configuration.set("DIRIGIBLE_SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS", "0");
        Files.createDirectories(source().getParent());
        Files.writeString(source(), "{}");

        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());
        when(repository.getResource(anyString())).thenAnswer(invocation -> {
            IResource resource = mock(IResource.class);
            when(resource.exists()).thenAnswer(exists -> Files.exists(source()));
            return resource;
        });
        when(definitionService.getAll()).thenReturn(List.of());

        List<Synchronizer<?, ?>> synchronizers = new ArrayList<>();
        synchronizers.add(synchronizer.mock);
        processor = new SynchronizationProcessor(repository, synchronizers, definitionService, watcher, new RegistryMutationTracker(),
                mock(TenantContext.class), mock(TenantArtefactLedgerService.class), mock(ObjectProvider.class));
        processor.prepareSynchronizers();
    }

    @AfterEach
    void resetTheConfiguration() {
        Configuration.remove("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_COUNT");
        Configuration.remove("DIRIGIBLE_SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS");
        Configuration.remove("DIRIGIBLE_SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS");
    }

    /** One attempt per phase in the pass that saw the file, no in-pass retry, and no retry armed. */
    @Test
    void aMissingEngineIsNotRetriedWithinThePass() {
        synchronizer.failure = () -> new ScriptEngineUnavailableException("project/handler.ts");

        processor.processSynchronizers();

        assertEquals(List.of("CREATE", "UPDATE", "START"), synchronizer.phases);
        assertEquals(ArtefactLifecycle.FAILED, synchronizer.artefact.getLifecycle());
        assertFalse(processor.isFailedRetryPending(), "Nothing a retry could heal is left");
    }

    /** Later passes - the FAILED retry and a pass carrying another change alike - leave it alone. */
    @Test
    void aMissingEngineIsNotAttemptedAgainUntilItsFileChanges() {
        synchronizer.failure = () -> new ScriptEngineUnavailableException("project/handler.ts");
        processor.processSynchronizers();
        synchronizer.phases.clear();
        alreadyParsed();

        processor.processSynchronizers();

        assertTrue(synchronizer.phases.isEmpty(), synchronizer.phases.toString());
    }

    /** Any other failure is still retried within the pass - the filter is the missing engine alone. */
    @Test
    void aTransientFailureIsStillRetriedWithinThePass() {
        synchronizer.failure = () -> new IllegalStateException("the broker is still taking its lease");

        processor.processSynchronizers();

        assertTrue(synchronizer.phases.size() > 3, synchronizer.phases.toString());
        assertTrue(processor.isFailedRetryPending(), "A transient failure is retried later as well");
    }

    /** Unpublished while the pass retries it, an artefact is no longer retried. */
    @Test
    void anArtefactUnpublishedDuringTheRetryIsNoLongerRetried() {
        synchronizer.failure = () -> {
            try {
                Files.deleteIfExists(source());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return new IllegalStateException("the broker is still taking its lease");
        };

        processor.processSynchronizers();

        assertEquals(List.of("CREATE", "UPDATE", "START"), synchronizer.phases);
    }

    private Path source() {
        return registry.resolve(LOCATION.substring(1));
    }

    /** The second pass finds the definition processed and the artefact as the first one left it. */
    private void alreadyParsed() {
        Definition definition = new Definition(LOCATION, "every", TYPE, "{}".getBytes());
        definition.setState(DefinitionState.PARSED);
        when(definitionService.findByKey(definition.getKey())).thenReturn(definition);
        when(synchronizer.mock.retrieve(LOCATION)).thenReturn(new ArrayList<>(List.of(synchronizer.artefact)));
        when(watcher.isModified()).thenReturn(true);
    }

    /**
     * A synchronizer whose artefact fails to start, recording each phase it is asked to complete it in,
     * and registering the failure through the callback as the job, listener and websocket ones do.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final class FailingSynchronizer {

        private final Synchronizer mock = mock(Synchronizer.class);
        private final List<String> phases = new ArrayList<>();
        private final Artefact artefact = new Artefact(LOCATION, "every", TYPE, "", null) {};
        private SynchronizerCallback callback;
        private Supplier<RuntimeException> failure;

        FailingSynchronizer() {
            ArtefactService service = mock(ArtefactService.class);
            when(service.getAll()).thenReturn(List.of());
            when(mock.getService()).thenReturn(service);
            when(mock.getArtefactType()).thenReturn(TYPE);
            when(mock.isAccepted(TYPE)).thenReturn(true);
            when(mock.isAccepted(any(Path.class), any())).thenReturn(true);
            when(mock.checksumContent(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
            doAnswer(invocation -> callback = invocation.getArgument(0)).when(mock)
                                                                        .setCallback(any());
            doAnswer(invocation -> {
                Artefact target = invocation.getArgument(0);
                target.setLifecycle(invocation.getArgument(1));
                target.setError(invocation.getArgument(2));
                return null;
            }).when(mock)
              .setStatus(any(), any(), any());
            try {
                when(mock.parse(anyString(), any())).thenReturn(new ArrayList<>(List.of(artefact)));
            } catch (java.text.ParseException e) {
                throw new IllegalStateException(e);
            }
            when(mock.complete(any(), any())).thenAnswer(invocation -> {
                TopologyWrapper<?> wrapper = invocation.getArgument(0);
                ArtefactPhase phase = invocation.getArgument(1);
                if (phase == ArtefactPhase.PREPARE) {
                    return true;
                }
                phases.add(phase.name());
                callback.registerState(mock, wrapper, ArtefactLifecycle.FAILED, failure.get());
                return false;
            });
        }
    }
}
