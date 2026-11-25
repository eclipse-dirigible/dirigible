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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.base.synchronizer.Synchronizer;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.components.initializers.definition.DefinitionState;
import org.eclipse.dirigible.components.initializers.synchronizer.tenants.TenantArtefactLedgerService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * A PARSED definition is parsed again only when its artefact rows are missing, and one that
 * legitimately yields no artefact costs no log line and no database write per pass (#8873).
 */
class SynchronizationProcessorParsedDefinitionTest {

    private static final String TYPE = "component";
    private static final String LOCATION = "/project/plain.service.ts";
    private static final String CONTENT = "export class Plain {}";

    @TempDir
    Path registry;

    private final DefinitionService definitionService = mock(DefinitionService.class);
    private final SynchronizationWatcher watcher = mock(SynchronizationWatcher.class);
    @SuppressWarnings("rawtypes")
    private final Synchronizer synchronizer = mock(Synchronizer.class);
    private final Definition definition = new Definition(LOCATION, "plain.service", TYPE, CONTENT.getBytes());
    private final ListAppender<ILoggingEvent> log = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(SynchronizationProcessor.class);

    private SynchronizationProcessor processor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void bootTheProcessor() throws IOException {
        log.start();
        logger.addAppender(log);

        Path source = registry.resolve(LOCATION.substring(1));
        Files.createDirectories(source.getParent());
        Files.writeString(source, CONTENT);

        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());
        when(definitionService.getAll()).thenReturn(List.of());
        definition.setState(DefinitionState.PARSED);
        when(definitionService.findByKey(definition.getKey())).thenReturn(definition);
        when(watcher.isModified()).thenReturn(true);

        ArtefactService<?, ?> service = mock(ArtefactService.class);
        when(service.getAll()).thenReturn(List.of());
        when(synchronizer.getService()).thenReturn(service);
        when(synchronizer.getArtefactType()).thenReturn(TYPE);
        when(synchronizer.isAccepted(TYPE)).thenReturn(true);
        when(synchronizer.isAccepted(any(Path.class), any())).thenReturn(true);
        when(synchronizer.checksumContent(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(synchronizer.complete(any(), any())).thenReturn(true);

        List<Synchronizer<?, ?>> synchronizers = new ArrayList<>();
        synchronizers.add(synchronizer);
        processor = new SynchronizationProcessor(repository, synchronizers, definitionService, watcher, new RegistryMutationTracker(),
                mock(TenantContext.class), mock(TenantArtefactLedgerService.class), mock(ObjectProvider.class));
        processor.prepareSynchronizers();
    }

    @AfterEach
    void detachTheLog() {
        logger.detachAppender(log);
    }

    /** Nothing to create: the definition stays PARSED, its row untouched, and no phase runs. */
    @Test
    @SuppressWarnings("unchecked")
    void aDefinitionThatYieldsNoArtefactStaysParsedWithoutAWrite() throws ParseException {
        when(synchronizer.retrieve(LOCATION)).thenReturn(new ArrayList<>());
        when(synchronizer.parse(anyString(), any())).thenReturn(new ArrayList<>());

        processor.processSynchronizers();

        verify(synchronizer, times(1)).parse(LOCATION, definition.getContent());
        verify(synchronizer, never()).complete(any(), any(ArtefactPhase.class));
        verify(definitionService, never()).save(any());
        assertEquals(DefinitionState.PARSED, definition.getState());
        assertTrue(log.list.stream()
                           .noneMatch(event -> event.getLevel()
                                                    .isGreaterOrEqual(Level.INFO)
                                   && event.getFormattedMessage()
                                           .contains(LOCATION)),
                log.list.toString());
    }

    /** Artefacts in the table are trusted, as before: no parse. */
    @Test
    @SuppressWarnings("unchecked")
    void aDefinitionWithItsArtefactIsNotParsedAgain() throws ParseException {
        Artefact artefact = new Artefact(LOCATION, "plain.service", TYPE, "", null) {};
        artefact.setLifecycle(ArtefactLifecycle.CREATED);
        when(synchronizer.retrieve(LOCATION)).thenReturn(new ArrayList<>(List.of(artefact)));

        processor.processSynchronizers();

        verify(synchronizer, never()).parse(anyString(), any());
        verify(definitionService, never()).save(any());
        assertEquals(DefinitionState.PARSED, definition.getState());
    }
}
