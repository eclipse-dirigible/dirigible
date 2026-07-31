/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

/**
 * The declaration rules: a valid file provisions per series; a broken file fails the parse; a
 * differing cross-module re-declaration fails that artefact loudly; DELETE never reaches the
 * counter store.
 */
class NumberSeriesSynchronizerTest {

    private static final String LOCATION = "/sales-invoices/sales-invoices.numbers";

    private NumberSeriesDeclarationService declarationService;
    private DocumentNumberService documentNumberService;
    private SynchronizerCallback callback;
    private NumberSeriesSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        declarationService = mock(NumberSeriesDeclarationService.class);
        documentNumberService = mock(DocumentNumberService.class);
        callback = mock(SynchronizerCallback.class);
        synchronizer = new NumberSeriesSynchronizer(declarationService, documentNumberService);
        synchronizer.setCallback(callback);
        when(declarationService.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    void parsesOneDeclarationPerSeriesEntry() throws ParseException {
        String content = "{\"series\": [{\"name\": \"Sales Invoice\", \"prefix\": \"SI\", \"size\": 10},"
                + " {\"name\": \"Credit Note\", \"size\": 10}]}";

        List<NumberSeriesDeclaration> declarations = synchronizer.parseImpl(LOCATION, content.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, declarations.size());
        NumberSeriesDeclaration first = declarations.get(0);
        assertEquals("Sales Invoice", first.getName());
        assertEquals("SI", first.getPrefix());
        assertEquals(10, first.getSize());
        assertEquals(LOCATION, first.getLocation());
        // An omitted prefix is a prefix-less continuous number, not a null.
        assertEquals("", declarations.get(1)
                                     .getPrefix());
    }

    @Test
    void rejectsMalformedJson() {
        assertParseFails("not json at all");
    }

    @Test
    void rejectsAFileWithoutSeries() {
        assertParseFails("{}");
        assertParseFails("{\"series\": []}");
    }

    @Test
    void rejectsANamelessSeries() {
        assertParseFails("{\"series\": [{\"prefix\": \"SI\", \"size\": 10}]}");
    }

    @Test
    void rejectsADuplicateSeriesNameWithinOneFile() {
        assertParseFails("{\"series\": [{\"name\": \"Sales Invoice\", \"size\": 10}, {\"name\": \"Sales Invoice\", \"size\": 8}]}");
    }

    @Test
    void rejectsAShapeTheSettingsPageWouldRefuse() {
        // The width leaves no room for a sequence after the prefix.
        assertParseFails("{\"series\": [{\"name\": \"Sales Invoice\", \"prefix\": \"INVOICE\", \"size\": 7}]}");
    }

    @Test
    void createProvisionsTheDeclaredSeries() throws Exception {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.NEW);
        when(declarationService.findAllByName("Sales Invoice")).thenReturn(List.of(declaration));

        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.CREATE));

        verify(documentNumberService).provision("Sales Invoice", "SI", 10);
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.CREATED));
    }

    @Test
    void anIdenticalDeclarationByAnotherModuleProvisionsIdempotently() throws Exception {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.NEW);
        NumberSeriesDeclaration twin = declaration("Sales Invoice", "SI", 10, "/other-module/other.numbers", ArtefactLifecycle.CREATED);
        when(declarationService.findAllByName("Sales Invoice")).thenReturn(List.of(declaration, twin));

        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.CREATE));

        verify(documentNumberService).provision("Sales Invoice", "SI", 10);
    }

    @Test
    void aDifferingDeclarationByAnotherModuleFailsLoudlyNamingBothModules() throws Exception {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.NEW);
        NumberSeriesDeclaration rival = declaration("Sales Invoice", "INV", 8, "/other-module/other.numbers", ArtefactLifecycle.CREATED);
        when(declarationService.findAllByName("Sales Invoice")).thenReturn(List.of(declaration, rival));

        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.CREATE));

        verify(documentNumberService, never()).provision(anyString(), anyString(), anyInt());
        verify(callback).addError(contains("/other-module/other.numbers"));
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.FAILED), contains(LOCATION));
    }

    @Test
    void aFailedDeclarationIsReevaluatedOnUpdateAndStaysDepleted() throws Exception {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.FAILED);
        NumberSeriesDeclaration rival = declaration("Sales Invoice", "INV", 8, "/other-module/other.numbers", ArtefactLifecycle.CREATED);
        when(declarationService.findAllByName("Sales Invoice")).thenReturn(List.of(declaration, rival));

        // The conflict persists: register FAILED again but STAY DEPLETED (return true) - returning
        // false would make the processor overwrite the conflict message with "undepleted" noise.
        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.UPDATE));
        verify(documentNumberService, never()).provision(anyString(), anyString(), anyInt());
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.FAILED), contains(LOCATION));

        // The other module re-declares identically: the next UPDATE pass heals the artefact.
        rival.setPrefix("SI");
        rival.setSize(10);
        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.UPDATE));
        verify(documentNumberService).provision("Sales Invoice", "SI", 10);
        verify(callback).registerState(eq(synchronizer), any(TopologyWrapper.class), eq(ArtefactLifecycle.UPDATED));
    }

    @Test
    void deleteRemovesOnlyTheDeclarationNeverTheSeries() throws Exception {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.CREATED);

        assertTrue(synchronizer.completeImpl(wrap(declaration), ArtefactPhase.DELETE));

        verify(declarationService).delete(declaration);
        verify(documentNumberService, never()).provision(anyString(), anyString(), anyInt());
        verify(documentNumberService, never()).setNext(anyString(), anyString(), anyLong());
        verify(documentNumberService, never()).setShape(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void cleanupReapsOnlyTheDeclaration() {
        NumberSeriesDeclaration declaration = declaration("Sales Invoice", "SI", 10, LOCATION, ArtefactLifecycle.CREATED);

        synchronizer.cleanupImpl(declaration);

        verify(declarationService).delete(declaration);
        verifyNoCounterWrites();
    }

    private void verifyNoCounterWrites() {
        try {
            verify(documentNumberService, never()).provision(anyString(), anyString(), anyInt());
            verify(documentNumberService, never()).setNext(anyString(), anyString(), anyLong());
            verify(documentNumberService, never()).setShape(anyString(), anyString(), anyString(), anyInt());
        } catch (Exception unexpected) {
            throw new AssertionError(unexpected);
        }
    }

    private void assertParseFails(String content) {
        assertThrows(ParseException.class, () -> synchronizer.parseImpl(LOCATION, content.getBytes(StandardCharsets.UTF_8)));
    }

    private static NumberSeriesDeclaration declaration(String name, String prefix, int size, String location, ArtefactLifecycle lifecycle) {
        NumberSeriesDeclaration declaration = new NumberSeriesDeclaration(location, name, prefix, size);
        declaration.updateKey();
        declaration.setLifecycle(lifecycle);
        return declaration;
    }

    private TopologyWrapper<NumberSeriesDeclaration> wrap(NumberSeriesDeclaration declaration) {
        return new TopologyWrapper<>(declaration, new HashMap<>(), synchronizer);
    }
}
