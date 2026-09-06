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

import java.util.function.Supplier;

import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.components.data.store.java.store.JavaEntityStore;

/**
 * Runs several entity writes as ONE unit: they share a transaction, so either all of them are
 * durable or none is.
 *
 * <p>
 * Each repository call is otherwise its own transaction, which is right for a single write and
 * wrong for an operation built out of several — an invoice created from a timesheet writes the
 * header, then its lines, then flips the source's status, and a failure on the third line used to
 * leave the first two, the header and the flipped source behind: a document that exists, is marked
 * as the month's billing, and is missing what it was for.
 *
 * <p>
 * Usage from client Java: {@code return UnitOfWork.call(() -> { var header = invoices.save(...);
 * ...; return header; });}
 *
 * <p>
 * Reads made inside the block see the block's own uncommitted writes, so a guard that re-reads the
 * row it just wrote behaves as it would after a commit. Blocks nest; the outermost one owns the
 * commit. The events the writes record are handed to the broker only once the whole unit committed.
 * The change history and document-number allocation deliberately stay outside it — both are records
 * of an attempt, not business state.
 */
public final class UnitOfWork {

    private UnitOfWork() {}

    /**
     * Runs the given work as one transaction and returns its result.
     *
     * @param <T> the result type
     * @param work the writes to perform together
     * @return whatever the work returned
     */
    public static <T> T call(Supplier<T> work) {
        return BeanProvider.getBean(JavaEntityStore.class)
                           .inUnitOfWork(work);
    }

    /**
     * Runs the given work as one transaction.
     *
     * @param work the writes to perform together
     */
    public static void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }

}
