/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Schema;
import org.eclipse.dirigible.components.data.structures.service.SchemaService;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.data.structures.service.ViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A schema whose data source cannot be resolved yet is recorded FAILED and retried on every pass -
 * never promoted to FATAL on the second attempt (#7248). The data source may be an artefact of a
 * project published on a later pass.
 */
class SchemasSynchronizerRetryTest {

    /** The data source the schema names and nothing has published yet. */
    private static final String LATER_DATA_SOURCE = "LaterDB";

    /** The synchronizer under test. */
    private SchemasSynchronizer synchronizer;

    /** The data sources, refusing the one the schema names. */
    private DataSourcesManager dataSourcesManager;

    /**
     * Wires the synchronizer over a data-sources double that knows no such data source and a callback
     * that writes the registered state onto the artefact, as the synchronization processor does.
     */
    @BeforeEach
    void setUp() {
        SchemaService schemaService = mock(SchemaService.class);
        when(schemaService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        dataSourcesManager = mock(DataSourcesManager.class);
        when(dataSourcesManager.getDataSource(LATER_DATA_SOURCE)).thenThrow(
                new IllegalArgumentException("DataSource [" + LATER_DATA_SOURCE + "] not found"));
        synchronizer =
                new SchemasSynchronizer(schemaService, dataSourcesManager, mock(TableService.class), mock(ViewService.class), "DefaultDB");

        SynchronizerCallback callback = mock(SynchronizerCallback.class);
        doAnswer(invocation -> {
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), "");
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class));
        doAnswer(invocation -> {
            Throwable cause = invocation.getArgument(3);
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), cause.getMessage());
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class), any(Throwable.class));
        synchronizer.setCallback(callback);
    }

    /**
     * The first refusal reads FAILED; the second used to read FATAL, after which the processor stripped
     * the schema from every later pass. Both now stay FAILED and not completed, so the pass retries.
     */
    @Test
    void aSchemaWhoseDataSourceIsNotThereYetStaysFailedAndIsRetried() {
        Schema schema = new Schema("/later/later.schema", "later", "", Set.of());
        schema.setDataSource(LATER_DATA_SOURCE);
        schema.setLifecycle(ArtefactLifecycle.NEW);
        TopologyWrapper<Schema> wrapper = new TopologyWrapper<>(schema, new HashMap<>(), synchronizer);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE),
                "An unresolved data source is not completed - the pass retries it");
        assertEquals(ArtefactLifecycle.FAILED, schema.getLifecycle());

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "The retry is refused the same way");
        assertEquals(ArtefactLifecycle.FAILED, schema.getLifecycle(), "A data source published later must never leave the schema FATAL");
        verify(dataSourcesManager, times(2)).getDataSource(LATER_DATA_SOURCE);
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the schema
     */
    private static Schema artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof Schema schema ? schema : null;
    }
}
