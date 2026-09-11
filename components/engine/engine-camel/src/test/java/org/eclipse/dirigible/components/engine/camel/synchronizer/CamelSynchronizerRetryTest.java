/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.camel.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.engine.camel.domain.Camel;
import org.eclipse.dirigible.components.engine.camel.processor.CamelProcessor;
import org.eclipse.dirigible.components.engine.camel.service.CamelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A route the Camel context refuses is recorded FAILED and retried on every pass until it starts -
 * never promoted to FATAL (#7248).
 */
class CamelSynchronizerRetryTest {

    /** What the context answers while a collaborator the route needs is not there yet. */
    private static final String ROUTE_REFUSAL = "No bean could be found in the registry for: laterDataSource";

    /** The synchronizer under test. */
    private CamelSynchronizer synchronizer;

    /** The processor whose route registration the tests make fail or succeed. */
    private CamelProcessor camelProcessor;

    /**
     * Wires the synchronizer over a processor double and a callback that writes the registered state
     * onto the artefact, as the synchronization processor does.
     */
    @BeforeEach
    void setUp() {
        CamelService camelService = mock(CamelService.class);
        when(camelService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        camelProcessor = mock(CamelProcessor.class);
        synchronizer = new CamelSynchronizer(camelService, camelProcessor);

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
        doAnswer(invocation -> {
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), invocation.getArgument(3));
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class), any(String.class));
        synchronizer.setCallback(callback);
    }

    /**
     * A refused create is FAILED and handed to the in-pass retry, whose START phase used to promote it
     * to FATAL within the very same pass; it now retries the route, and it stays FAILED while refused.
     */
    @Test
    void aRefusedRouteStaysFailedAndIsRetriedNotPromotedToFatal() {
        Camel camel = camel(ArtefactLifecycle.NEW);
        TopologyWrapper<Camel> wrapper = wrapper(camel);
        doThrow(new IllegalArgumentException(ROUTE_REFUSAL)).when(camelProcessor)
                                                            .onCreateOrUpdate(camel);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "A refused create is not completed - the pass retries it");
        assertEquals(ArtefactLifecycle.FAILED, camel.getLifecycle());
        assertEquals(ROUTE_REFUSAL, camel.getError());

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "A refused start is not completed either");
        assertEquals(ArtefactLifecycle.FAILED, camel.getLifecycle(), "A transiently refused route must never read FATAL");
        verify(camelProcessor, times(2)).onCreateOrUpdate(camel);
    }

    /**
     * Once the context accepts, the very same FAILED artefact starts, reads CREATED and carries no
     * stale error.
     */
    @Test
    void aFailedRouteHealsOnceTheContextAccepts() {
        Camel camel = camel(ArtefactLifecycle.FAILED);
        camel.setError(ROUTE_REFUSAL);
        TopologyWrapper<Camel> wrapper = wrapper(camel);
        doNothing().when(camelProcessor)
                   .onCreateOrUpdate(camel);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "A start the context accepted completes");
        assertEquals(ArtefactLifecycle.CREATED, camel.getLifecycle(), "The healed route must read CREATED");
        assertEquals("", camel.getError(), "The healed route must carry no stale error");
        verify(camelProcessor).onCreateOrUpdate(camel);
    }

    private static Camel camel(ArtefactLifecycle lifecycle) {
        Camel camel = new Camel();
        camel.setLocation("/retry/probe.camel");
        camel.setName("probe.camel");
        camel.setType(Camel.ARTEFACT_TYPE);
        camel.updateKey();
        camel.setContent("<routes/>");
        camel.setLifecycle(lifecycle);
        return camel;
    }

    private TopologyWrapper<Camel> wrapper(Camel camel) {
        return new TopologyWrapper<>(camel, new HashMap<>(), synchronizer);
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the route
     */
    private static Camel artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof Camel camel ? camel : null;
    }
}
