/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.domain.ListenerKind;
import org.eclipse.dirigible.components.listeners.service.ListenerService;
import org.eclipse.dirigible.components.listeners.service.ListenersManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * A listener whose subscription the broker refuses is recorded FAILED and retried on every pass
 * until it subscribes - never CREATED while nothing runs, never promoted to FATAL (#7248).
 */
class ListenerSynchronizerRetryTest {

    /** What the broker answers while it is not accepting connections yet. */
    private static final String BROKER_REFUSAL = "Failed to start listener for [my-topic]";

    /** The synchronizer under test. */
    private ListenerSynchronizer synchronizer;

    /** The manager whose start the tests make fail or succeed. */
    private ListenersManager listenersManager;

    /** The callback, persisting state the way the synchronization processor does. */
    private SynchronizerCallback callback;

    /**
     * Wires the synchronizer over a manager double and a callback that writes the registered state onto
     * the artefact, as the synchronization processor does.
     */
    @BeforeEach
    void setUp() {
        ListenerService listenerService = mock(ListenerService.class);
        when(listenerService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        listenersManager = mock(ListenersManager.class);

        synchronizer = new ListenerSynchronizer();
        ReflectionTestUtils.setField(synchronizer, "listenerService", listenerService);
        ReflectionTestUtils.setField(synchronizer, "listenersManager", listenersManager);

        callback = mock(SynchronizerCallback.class);
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
     * The first pass: a refused start is FAILED with its cause and not completed - not CREATED with
     * nothing running, which is how the Registry read a listener that never subscribed.
     */
    @Test
    void aRefusedStartIsRecordedFailedNotCreated() {
        Listener listener = listener(ArtefactLifecycle.NEW);
        TopologyWrapper<Listener> wrapper = wrapper(listener);
        doThrow(new IllegalStateException(BROKER_REFUSAL)).when(listenersManager)
                                                          .startListener(listener);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "A refused start is not completed - the pass retries it");
        assertEquals(ArtefactLifecycle.FAILED, listener.getLifecycle(), "A listener that never subscribed must read FAILED, not CREATED");
        assertEquals(BROKER_REFUSAL, listener.getError(), "The recorded error must be the broker's refusal");
        assertNotEquals(Boolean.TRUE, listener.getRunning(), "Nothing started, so nothing may read as running");
    }

    /**
     * The passes after: a FAILED listener the broker still refuses stays FAILED and keeps being
     * retried. Before the fix the second attempt registered FATAL, after which the processor stripped
     * the artefact from every later pass.
     */
    @Test
    void aFailedListenerTheBrokerStillRefusesStaysFailedAndIsRetried() {
        Listener listener = listener(ArtefactLifecycle.FAILED);
        TopologyWrapper<Listener> wrapper = wrapper(listener);
        doThrow(new IllegalStateException(BROKER_REFUSAL)).when(listenersManager)
                                                          .startListener(listener);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "A start the broker refused is not completed");
        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "The next attempt is refused the same way");

        assertEquals(ArtefactLifecycle.FAILED, listener.getLifecycle(), "A transiently refused listener must never read FATAL");
        assertNotEquals(Boolean.TRUE, listener.getRunning(), "Nothing started, so nothing may read as running");
        verify(listenersManager, times(2)).startListener(listener);
    }

    /**
     * Once the broker accepts, the very same FAILED artefact subscribes, reads CREATED and carries no
     * stale error.
     */
    @Test
    void aFailedListenerHealsOnceTheBrokerAccepts() {
        Listener listener = listener(ArtefactLifecycle.FAILED);
        listener.setError(BROKER_REFUSAL);
        TopologyWrapper<Listener> wrapper = wrapper(listener);
        doNothing().when(listenersManager)
                   .startListener(listener);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "A start the broker accepted completes");
        assertEquals(ArtefactLifecycle.CREATED, listener.getLifecycle(), "The healed listener must read CREATED");
        assertEquals("", listener.getError(), "The healed listener must carry no stale error");
        assertEquals(Boolean.TRUE, listener.getRunning(), "The healed listener is running");
    }

    /**
     * A listener that is already running is left alone by the START phase - the retry is for what is
     * not running, not a re-subscription on every pass.
     */
    @Test
    void aRunningListenerIsNotStartedAgain() {
        Listener listener = listener(ArtefactLifecycle.CREATED);
        listener.setRunning(true);
        TopologyWrapper<Listener> wrapper = wrapper(listener);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.START));
        verify(listenersManager, times(0)).startListener(any());
        assertEquals(ArtefactLifecycle.CREATED, listener.getLifecycle());
    }

    private static Listener listener(ArtefactLifecycle lifecycle) {
        Listener listener = new Listener("/retry/probe.listener", "my-topic", "", "retry/handler.js", ListenerKind.TOPIC);
        listener.setLifecycle(lifecycle);
        listener.setRunning(false);
        return listener;
    }

    private TopologyWrapper<Listener> wrapper(Listener listener) {
        return new TopologyWrapper<>(listener, new HashMap<>(), synchronizer);
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the listener
     */
    private static Listener artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof Listener listener ? listener : null;
    }
}
