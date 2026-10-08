/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.repository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;
import org.eclipse.dirigible.sdk.db.ConcurrentWriteException;
import org.eclipse.dirigible.sdk.db.TargetDeleteRule;
import org.eclipse.dirigible.sdk.extensions.Extensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * A delete whose cascade collides with a concurrent writer (dirigible #7716): the cascade loaded
 * the referencing rows, and a posting handler on its own listener thread replaced them before the
 * delete flushed, so the delete counted one row and deleted none. The delete that owns its
 * transaction runs once more from a fresh read; a second collision, or one inside a caller's own
 * unit of work, reaches the caller as the conflict.
 */
class JavaRepositoryConcurrentDeleteTest {

    private static final String TARGET = Payment.class.getName();

    private JavaEntityStore store;

    private TargetDeleteRule rule;

    private MockedStatic<BeanProvider> beans;

    private MockedStatic<Extensions> extensions;

    private PaymentRepository repository;

    @BeforeEach
    void setUp() {
        store = mock(JavaEntityStore.class);
        rule = mock(TargetDeleteRule.class);
        // The unit of work runs its block as the real one does; the store stays a mock underneath.
        when(store.inUnitOfWork(any())).thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        beans = mockStatic(BeanProvider.class);
        beans.when(BeanProvider::isInitialzed)
             .thenReturn(true);
        beans.when(() -> BeanProvider.getBean(JavaEntityStore.class))
             .thenReturn(store);
        extensions = mockStatic(Extensions.class);
        extensions.when(() -> Extensions.find(TargetDeleteRule.class))
                  .thenReturn(List.of(rule));
        repository = new PaymentRepository(store);
    }

    @AfterEach
    void tearDown() {
        extensions.close();
        beans.close();
    }

    @Test
    void aCollidingCascadeIsRunOnceMoreFromAFreshRead() {
        doThrow(collision()).doNothing()
                            .when(store)
                            .deleteById(Payment.class, 7);

        repository.deleteById(7);

        verify(rule, times(2)).countRestricting(TARGET, 7);
        verify(rule, times(2)).release(TARGET, 7);
        verify(store, times(2)).deleteById(Payment.class, 7);
    }

    @Test
    void aSecondCollisionIsTheCallersConflict() {
        ConcurrentWriteException second = collision();
        doThrow(collision()).doThrow(second)
                            .when(store)
                            .deleteById(Payment.class, 7);

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> repository.deleteById(7));

        assertSame(second, thrown);
        verify(store, times(2)).deleteById(Payment.class, 7);
    }

    /** Inside a caller's unit the earlier writes of that unit cannot be repeated from here. */
    @Test
    void aCollisionInsideACallersUnitIsNotRetried() {
        when(store.isInUnitOfWork()).thenReturn(true);
        ConcurrentWriteException first = collision();
        doAnswer(invocation -> {
            throw first;
        }).when(store)
          .deleteById(Payment.class, 7);

        ConcurrentWriteException thrown = assertThrows(ConcurrentWriteException.class, () -> repository.deleteById(7));

        assertSame(first, thrown);
        verify(store, times(1)).deleteById(Payment.class, 7);
    }

    private static ConcurrentWriteException collision() {
        return new ConcurrentWriteException("changed by another write", new IllegalStateException("expected row count 1 but was 0"));
    }

    static class Payment {
    }

    static class PaymentRepository extends JavaRepository<Payment> {

        private final JavaEntityStore store;

        PaymentRepository(JavaEntityStore store) {
            super(Payment.class);
            this.store = store;
        }

        @Override
        protected JavaEntityStore store() {
            return store;
        }
    }
}
