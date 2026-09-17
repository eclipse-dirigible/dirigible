/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;

/**
 * The reconciliation pass is the only thing that retries a subscription the broker refused, so it
 * has to survive everything a consumer can do to it. One that throws must not stop the ones behind
 * it, and nothing may escape the pass at all: it is submitted to {@code scheduleWithFixedDelay},
 * which cancels a task that throws permanently and without a word - turning the watchdog off for
 * the rest of the process, which is precisely the state issue #7217 is about.
 */
class JavaConsumersReconcilerTest {

    /** Counts its reconciliations, and optionally fails them. */
    private static final class CountingConsumer implements JavaClassConsumer {

        private final AtomicInteger reconciliations = new AtomicInteger();
        private final RuntimeException failure;

        private CountingConsumer(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public boolean accepts(Class<?> clazz) {
            return false;
        }

        @Override
        public void onClassLoaded(LoadedClass info) {
            // Not part of this test.
        }

        @Override
        public void onClassUnloaded(LoadedClass info) {
            // Not part of this test.
        }

        @Override
        public void reconcile() {
            reconciliations.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
        }
    }

    @Test
    void everyConsumerIsReconciled() {
        CountingConsumer first = new CountingConsumer(null);
        CountingConsumer second = new CountingConsumer(null);

        new JavaConsumersReconciler(List.of(first, second)).reconcileAll();

        assertEquals(1, first.reconciliations.get());
        assertEquals(1, second.reconciliations.get());
    }

    @Test
    void aThrowingConsumerNeitherStopsTheOthersNorEscapesThePass() {
        CountingConsumer throwing = new CountingConsumer(new IllegalStateException("boom"));
        CountingConsumer behindIt = new CountingConsumer(null);
        JavaConsumersReconciler reconciler = new JavaConsumersReconciler(List.of(throwing, behindIt));

        assertDoesNotThrow(reconciler::reconcileAll);

        assertEquals(1, behindIt.reconciliations.get(), "a consumer must not lose its retry because an earlier one failed");
    }
}
