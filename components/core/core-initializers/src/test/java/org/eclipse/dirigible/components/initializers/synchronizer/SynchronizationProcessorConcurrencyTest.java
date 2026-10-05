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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.components.initializers.definition.Definition;
import org.eclipse.dirigible.components.initializers.definition.DefinitionService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Two callers that both pass the "is a pass needed?" check - the scheduled job and a
 * {@code forceProcessSynchronizers()} caller - must not both run a pass (#7655).
 */
class SynchronizationProcessorConcurrencyTest {

    private static final long WAIT_SECONDS = 10;

    @TempDir
    Path registry;

    @Test
    void twoCallersPastTheCheckRunOnePass() throws Exception {
        SynchronizationWatcher watcher = mock(SynchronizationWatcher.class);
        // Both callers read "modified" together, so both pass the check before either claims the pass.
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        AtomicInteger modifiedReads = new AtomicInteger();
        when(watcher.isModified()).thenAnswer(invocation -> {
            if (modifiedReads.incrementAndGet() <= 2) {
                bothChecked.await(WAIT_SECONDS, TimeUnit.SECONDS);
                return true;
            }
            return false;
        });

        // The pass holds until a caller has returned, so a second pass could only run alongside it.
        CountDownLatch callerReturned = new CountDownLatch(1);
        AtomicInteger passesRunning = new AtomicInteger();
        AtomicInteger maxPassesRunning = new AtomicInteger();
        AtomicInteger passes = new AtomicInteger();
        DefinitionService definitionService = mock(DefinitionService.class);
        when(definitionService.getAll()).thenAnswer(invocation -> {
            passes.incrementAndGet();
            maxPassesRunning.accumulateAndGet(passesRunning.incrementAndGet(), Math::max);
            try {
                callerReturned.await(WAIT_SECONDS, TimeUnit.SECONDS);
            } finally {
                passesRunning.decrementAndGet();
            }
            return List.<Definition>of();
        });

        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());

        SynchronizationProcessor processor =
                new SynchronizationProcessor(repository, new ArrayList<>(), definitionService, watcher, new RegistryMutationTracker());
        processor.prepareSynchronizers();

        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            Runnable caller = () -> {
                processor.processSynchronizers();
                callerReturned.countDown();
            };
            Future<?> first = callers.submit(caller);
            Future<?> second = callers.submit(caller);
            first.get(WAIT_SECONDS * 3, TimeUnit.SECONDS);
            second.get(WAIT_SECONDS * 3, TimeUnit.SECONDS);
        } finally {
            callers.shutdownNow();
        }

        assertEquals(1, maxPassesRunning.get(), "passes running at the same time");
        assertEquals(1, passes.get(), "passes run");
        verify(watcher, times(1)).reset();
        assertFalse(processor.isSynchronizationRunning());
    }
}
