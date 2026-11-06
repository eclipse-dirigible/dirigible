/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.outbox.EventOutbox;
import org.eclipse.dirigible.sdk.db.ConcurrentWriteException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.exception.LockAcquisitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;

/**
 * What {@code inUnitOfWork} owes its caller when the block fails: the transaction is rolled back
 * explicitly, before the session is closed, whatever the block threw. An {@link Error} — an
 * assertion, a {@link StackOverflowError} from a handler that recursed, an OOME — used to skip the
 * rollback and reach the {@code finally}, closing the session with the transaction still open and
 * leaving the outcome to the connection pool.
 */
class JavaEntityStoreUnitOfWorkTest {

    private Session session;

    private Transaction transaction;

    private JavaEntityStore store;

    @BeforeEach
    void setUp() {
        session = mock(Session.class);
        transaction = mock(Transaction.class);
        when(session.beginTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);

        SessionFactory sessionFactory = mock(SessionFactory.class);
        when(sessionFactory.openSession()).thenReturn(session);
        JavaEntityManager entityManager = mock(JavaEntityManager.class);
        when(entityManager.getSessionFactory()).thenReturn(sessionFactory);

        store = new JavaEntityStore(entityManager, mock(EventOutbox.class));
        pauses = new ArrayList<>();
        store.setPauser(pauses::add);
    }

    /** The pauses taken between deadlock repeats, recorded instead of slept. */
    private List<Long> pauses;

    @Test
    void commitsWhenTheBlockReturns() {
        assertEquals("done", store.inUnitOfWork(() -> "done"));

        verify(transaction).commit();
        verify(transaction, never()).rollback();
        verify(session).close();
    }

    @Test
    void rollsBackWhenTheBlockThrowsARuntimeException() {
        RuntimeException failure = new IllegalStateException("boom");

        RuntimeException thrown = assertThrows(IllegalStateException.class, () -> store.inUnitOfWork(() -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertRolledBackBeforeClose();
    }

    @Test
    void rollsBackWhenTheBlockThrowsAnError() {
        Error failure = new StackOverflowError();

        Error thrown = assertThrows(StackOverflowError.class, () -> store.inUnitOfWork(() -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertRolledBackBeforeClose();
    }

    /**
     * A rollback that itself fails must not replace the failure the caller came to see — it is attached
     * to it as suppressed.
     */
    @Test
    void keepsTheOriginalErrorWhenTheRollbackAlsoFails() {
        Error failure = new AssertionError("assertion in the block");
        RuntimeException rollbackFailure = new IllegalStateException("connection already gone");
        org.mockito.Mockito.doThrow(rollbackFailure)
                           .when(transaction)
                           .rollback();

        Error thrown = assertThrows(AssertionError.class, () -> store.inUnitOfWork(() -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(rollbackFailure, thrown.getSuppressed()[0]);
        verify(session).close();
    }

    /** A nested block joins the outer one, which alone owns the commit and the rollback. */
    @Test
    void anInnerBlockDoesNotCommitOnItsOwn() {
        assertEquals("inner", store.inUnitOfWork(() -> store.inUnitOfWork(() -> "inner")));

        verify(session).beginTransaction();
        verify(transaction).commit();
    }

    /**
     * The thread must be left without a unit of work even when the block failed with an Error, or every
     * later call on this thread would join a closed session.
     */
    @Test
    void clearsTheThreadAfterAnError() {
        assertThrows(StackOverflowError.class, () -> store.inUnitOfWork(() -> {
            throw new StackOverflowError();
        }));

        assertEquals("after", store.inUnitOfWork(() -> "after"));
        verify(session, org.mockito.Mockito.times(2)).beginTransaction();
    }

    /**
     * A commit that finds a row it read already changed or removed by another writer - a cascade
     * deleting a line a posting handler replaced meanwhile (#7716) - is a conflict the caller can
     * repeat, not a server fault: the unit is rolled back and the caller is handed a
     * {@link ConcurrentWriteException} carrying the persistence failure.
     */
    @Test
    void aCommitThatFindsAConcurrentWriteIsAConflict() {
        OptimisticLockException stale = new OptimisticLockException("Unexpected row count (expected row count 1 but was 0)");
        org.mockito.Mockito.doThrow(stale)
                           .when(transaction)
                           .commit();

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWork(() -> "work"));

        assertSame(stale, thrown.getCause());
        verify(transaction).rollback();
        verify(session).close();
    }

    /** The same failure raised by a flush inside the block, wrapped or not, reads the same way. */
    @Test
    void aFlushThatFindsAConcurrentWriteIsAConflict() {
        RuntimeException wrapped =
                new IllegalStateException("flush failed", new StaleObjectStateException("journal_JournalEntryItemEntity", 54));

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWork(() -> {
            throw wrapped;
        }));

        assertSame(wrapped, thrown.getCause());
        assertRolledBackBeforeClose();
    }

    /** The unit is visible to the code it runs, and to nothing after it. */
    @Test
    void reportsWhetherAUnitIsOpen() {
        assertFalse(store.isInUnitOfWork());
        assertTrue(store.inUnitOfWork(store::isInUnitOfWork));
        assertFalse(store.isInUnitOfWork());
    }

    /**
     * A row lock outside a unit would be released by its own statement's commit - refused, not a no-op.
     */
    @Test
    void refusesARowLockOutsideAUnit() {
        assertThrows(IllegalStateException.class, () -> store.lockForUpdate(Object.class, 1));
    }

    /** The share lock of a reference check (#8252) is refused outside a unit, as the update lock is. */
    @Test
    void refusesAShareLockOutsideAUnit() {
        assertThrows(IllegalStateException.class, () -> store.lockForShare("gen.staff.data.employee.EmployeeEntity", 1));
    }

    /**
     * A row lock another writer held - the victim a database picked to break a deadlock - is a conflict
     * the caller can repeat, not a server fault (#8252).
     */
    @Test
    void aDeadlockVictimIsAConflict() {
        LockAcquisitionException deadlock = new LockAcquisitionException("deadlock detected", null);

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWork(() -> {
            throw deadlock;
        }));

        assertSame(deadlock, thrown.getCause());
        assertRolledBackBeforeClose();
    }

    /** So is a lock wait that timed out. */
    @Test
    void aLockTimeoutIsAConflict() {
        LockTimeoutException timeout = new LockTimeoutException("lock wait timed out");

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWork(() -> {
            throw timeout;
        }));

        assertSame(timeout, thrown.getCause());
        assertRolledBackBeforeClose();
    }

    /**
     * A unit the database rolled back to break a deadlock is run once more (#8341): nothing it wrote
     * survived and nothing it read was wrong, and by then the other transaction has committed.
     */
    @Test
    void aDeadlockVictimIsRunOnceMore() {
        AtomicInteger attempts = new AtomicInteger();

        String result = store.inUnitOfWorkRetryingDeadlock(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw deadlock("40001");
            }
            return "saved";
        });

        assertEquals("saved", result);
        assertEquals(2, attempts.get());
        verify(transaction).rollback();
        verify(transaction).commit();
        verify(session, times(2)).close();
        assertEquals(1, pauses.size());
    }

    /** PostgreSQL reports a deadlock as 40P01. */
    @Test
    void aPostgreSqlDeadlockVictimIsRunOnceMore() {
        AtomicInteger attempts = new AtomicInteger();

        assertEquals("saved", store.inUnitOfWorkRetryingDeadlock(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw deadlock("40P01");
            }
            return "saved";
        }));
        assertEquals(2, attempts.get());
    }

    /**
     * A repeat that is chosen as the victim again is repeated again (#8507): the survivor of the first
     * deadlock is still taking locks, so the repeat can collide with it once more. Each repeat waits
     * first, a little longer than the one before.
     */
    @Test
    void aRepeatedDeadlockVictimIsRunAgainAfterAPause() {
        AtomicInteger attempts = new AtomicInteger();

        assertEquals("saved", store.inUnitOfWorkRetryingDeadlock(() -> {
            if (attempts.incrementAndGet() < 4) {
                throw deadlock("40001");
            }
            return "saved";
        }));

        assertEquals(4, attempts.get());
        verify(transaction, times(3)).rollback();
        verify(transaction).commit();
        assertEquals(3, pauses.size());
        for (int i = 0; i < pauses.size(); i++) {
            long ceiling = JavaEntityStore.DEADLOCK_PAUSE_MILLIS << i;
            long pause = pauses.get(i);
            assertTrue(pause >= ceiling / 2 && pause <= ceiling, "pause " + (i + 1) + " was " + pause + " ms");
        }
    }

    /** A write that loses every attempt is the caller's conflict - the attempts are bounded. */
    @Test
    void aDeadlockOnEveryAttemptIsTheCallersConflict() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWorkRetryingDeadlock(() -> {
            attempts.incrementAndGet();
            throw deadlock("40001");
        }));

        assertEquals(JavaEntityStore.DEADLOCK_ATTEMPTS, attempts.get());
        assertEquals(JavaEntityStore.DEADLOCK_ATTEMPTS - 1, pauses.size());
        verify(transaction, times(JavaEntityStore.DEADLOCK_ATTEMPTS)).rollback();
        verify(transaction, never()).commit();
    }

    /**
     * An interrupt during the pause ends the repeats: the conflict reaches the caller, interrupt kept.
     */
    @Test
    void anInterruptedPauseIsTheCallersConflict() {
        AtomicInteger attempts = new AtomicInteger();
        store.setPauser(millis -> {
            throw new InterruptedException("shutting down");
        });

        try {
            assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWorkRetryingDeadlock(() -> {
                attempts.incrementAndGet();
                throw deadlock("40001");
            }));
            assertEquals(1, attempts.get());
            assertTrue(Thread.currentThread()
                             .isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    /** The pauses stay within their doubling bounds, so the repeats cost well under a second. */
    @Test
    void thePausesDoubleAndStayBounded() {
        long total = 0;
        for (int attempt = 1; attempt < JavaEntityStore.DEADLOCK_ATTEMPTS; attempt++) {
            long ceiling = JavaEntityStore.DEADLOCK_PAUSE_MILLIS << (attempt - 1);
            for (int sample = 0; sample < 200; sample++) {
                long pause = JavaEntityStore.deadlockPause(attempt);
                assertTrue(pause >= ceiling / 2 && pause <= ceiling, "attempt " + attempt + " paused " + pause + " ms");
            }
            total += ceiling;
        }
        assertTrue(total < 1000, "worst case " + total + " ms");
    }

    /**
     * A lock wait that timed out is not repeated: it may have waited for a long-running writer, and
     * repeating it would double the caller's wait.
     */
    @Test
    void aLockTimeoutIsNotRunAgain() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWorkRetryingDeadlock(() -> {
            attempts.incrementAndGet();
            throw new LockTimeoutException("lock wait timed out", new SQLException("Timeout trying to lock table", "HYT00"));
        }));

        assertEquals(1, attempts.get());
    }

    /** Nor is a write that found a row it read already changed - that is a conflict over data. */
    @Test
    void aStaleRowIsNotRunAgain() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWorkRetryingDeadlock(() -> {
            attempts.incrementAndGet();
            throw new StaleObjectStateException("journal_JournalEntryItemEntity", 54);
        }));

        assertEquals(1, attempts.get());
    }

    /**
     * Joining an open unit runs the block once: only the owner of the whole transaction can repeat it,
     * and the outer unit's earlier writes are not this block's to repeat.
     */
    @Test
    void aJoiningBlockIsNotRunAgain() {
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(ConcurrentWriteException.class, () -> store.inUnitOfWork(() -> store.inUnitOfWorkRetryingDeadlock(() -> {
            attempts.incrementAndGet();
            throw deadlock("40001");
        })));

        assertEquals(1, attempts.get());
        verify(session).beginTransaction();
    }

    /**
     * What H2 raises for the victim it picked: the SQLSTATE travels as the cause of the lock failure.
     */
    private static RuntimeException deadlock(String sqlState) {
        return new PessimisticLockException("Deadlock detected. The current transaction was rolled back.",
                new LockAcquisitionException("deadlock detected", new SQLException("Deadlock detected", sqlState)));
    }

    private void assertRolledBackBeforeClose() {
        InOrder order = inOrder(transaction, session);
        order.verify(transaction)
             .rollback();
        order.verify(session)
             .close();
        verify(transaction, never()).commit();
    }

}
