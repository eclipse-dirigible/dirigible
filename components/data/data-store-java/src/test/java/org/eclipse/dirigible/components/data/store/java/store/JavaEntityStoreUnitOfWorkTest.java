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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.components.data.store.java.outbox.EventOutbox;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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
    }

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

    private void assertRolledBackBeforeClose() {
        InOrder order = inOrder(transaction, session);
        order.verify(transaction)
             .rollback();
        order.verify(session)
             .close();
        verify(transaction, never()).commit();
    }

}
