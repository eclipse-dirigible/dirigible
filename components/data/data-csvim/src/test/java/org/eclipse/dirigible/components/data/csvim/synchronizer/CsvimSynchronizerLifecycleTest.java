/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.csvim.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.data.csvim.domain.Csvim;
import org.eclipse.dirigible.components.data.csvim.processor.CsvimProcessor;
import org.eclipse.dirigible.components.data.csvim.service.CsvFileService;
import org.eclipse.dirigible.components.data.csvim.service.CsvimService;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * A pass runs every artefact through CREATE, UPDATE and START. An imported seed must come out of
 * that pass CREATED (or UPDATED), and only a DELETE phase may record it DELETED.
 */
class CsvimSynchronizerLifecycleTest {

    /** The synchronizer under test. */
    private CsvimSynchronizer synchronizer;

    /**
     * Wires the synchronizer with a callback that applies the registered state to the artefact, as the
     * synchronization processor does.
     */
    @BeforeEach
    void setUp() {
        synchronizer = new CsvimSynchronizer(mock(CsvimService.class), mock(DataSourcesManager.class), mock(CsvimProcessor.class),
                "SystemDB", mock(CsvFileService.class));
        SynchronizerCallback callback = mock(SynchronizerCallback.class);
        doAnswer(invocation -> {
            TopologyWrapper<?> wrapper = invocation.getArgument(1);
            wrapper.getArtefact()
                   .setLifecycle(invocation.getArgument(2));
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class));
        synchronizer.setCallback(callback);
    }

    /**
     * A new seed is imported in CREATE and stays CREATED through the rest of the pass.
     */
    @Test
    void aNewSeedReadsCreatedAfterThePass() {
        TopologyWrapper<Csvim> wrapper = wrapper(ArtefactLifecycle.NEW);

        runPass(wrapper);

        assertEquals(ArtefactLifecycle.CREATED, wrapper.getArtefact()
                                                       .getLifecycle(),
                "the UPDATE phase must not record an imported seed DELETED");
    }

    /**
     * A modified seed is imported in UPDATE and stays UPDATED through the rest of the pass.
     */
    @Test
    void aModifiedSeedReadsUpdatedAfterThePass() {
        TopologyWrapper<Csvim> wrapper = wrapper(ArtefactLifecycle.MODIFIED);

        runPass(wrapper);

        assertEquals(ArtefactLifecycle.UPDATED, wrapper.getArtefact()
                                                       .getLifecycle());
    }

    /**
     * The DELETE phase still records a removed seed DELETED.
     */
    @Test
    void theDeletePhaseRecordsDeleted() {
        TopologyWrapper<Csvim> wrapper = wrapper(ArtefactLifecycle.CREATED);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.DELETE));

        assertEquals(ArtefactLifecycle.DELETED, wrapper.getArtefact()
                                                       .getLifecycle());
    }

    /**
     * The phases a pass runs, in its order.
     *
     * @param wrapper the seed
     */
    private void runPass(TopologyWrapper<Csvim> wrapper) {
        for (ArtefactPhase phase : new ArtefactPhase[] {ArtefactPhase.CREATE, ArtefactPhase.UPDATE, ArtefactPhase.START}) {
            assertTrue(synchronizer.completeImpl(wrapper, phase), "phase " + phase + " must complete");
        }
    }

    /**
     * A seed with no files, so the import has nothing to read.
     *
     * @param lifecycle the lifecycle it enters the pass with
     * @return the wrapper
     */
    private TopologyWrapper<Csvim> wrapper(ArtefactLifecycle lifecycle) {
        Csvim csvim = new Csvim();
        csvim.setLocation("/project/seed.csvim");
        csvim.setFiles(new ArrayList<>());
        csvim.setLifecycle(lifecycle);
        return new TopologyWrapper<>(csvim, new HashMap<>(), synchronizer);
    }
}
