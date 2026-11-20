/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DeadlockVictimRetryTest {

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    /**
     * A {@code @Listener} method is invoked by reflection, so its failure arrives wrapped in an
     * InvocationTargetException - the SQLSTATE is found anywhere in the chain.
     */
    @Test
    void aVictimIsRecognisedThroughTheReflectiveWrapper() {
        Throwable wrapped = new InvocationTargetException(new IllegalStateException("conflict", new SQLException("deadlock", "40P01")));

        assertTrue(DeadlockVictimRetry.isLockConflictVictim(wrapped));
    }

    @Test
    void anotherSqlStateIsNoVictim() {
        assertFalse(DeadlockVictimRetry.isLockConflictVictim(new IllegalStateException(new SQLException("unique", "23505"))));
        assertFalse(DeadlockVictimRetry.isLockConflictVictim(new IllegalStateException("no SQL at all")));
    }

    @Test
    void thePauseDoublesAndStaysInTheUpperHalf() {
        for (int run = 1; run < DeadlockVictimRetry.ATTEMPTS; run++) {
            long ceiling = DeadlockVictimRetry.PAUSE_MILLIS << (run - 1);
            long pause = DeadlockVictimRetry.pause(run);
            assertTrue(pause >= ceiling / 2 && pause <= ceiling, "run " + run + " paused " + pause);
        }
    }

    /** An interrupted wait ends the repeats: the victim's failure goes on to the broker, as before. */
    @Test
    void anInterruptedPauseHandsTheFailureBack() {
        AtomicInteger runs = new AtomicInteger();
        Exception deadlock = new IllegalStateException(new SQLException("deadlock", "40001"));
        DeadlockVictimRetry retry = new DeadlockVictimRetry(millis -> {
            throw new InterruptedException();
        });

        Exception thrown = assertThrows(Exception.class, () -> retry.run(() -> {
            runs.incrementAndGet();
            throw deadlock;
        }, "label"));

        assertSame(deadlock, thrown);
        assertEquals(1, runs.get());
        assertTrue(Thread.currentThread()
                         .isInterrupted(),
                "the interrupt is kept for the broker thread's owner");
    }
}
