/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.numbering;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * The write transaction open on the current thread, which a number allocation joins (#8113). Drawn
 * on that transaction's connection, a number commits with the write it was drawn for, and a write
 * the database refuses afterwards - a duplicate key, a value its column cannot hold - takes the
 * counter back with it instead of leaving a permanent hole in a series that must be gap-free.
 *
 * <p>
 * The entity store's unit of work implements it. It is declared here because the store depends on
 * this module through the SDK, not the other way round.
 */
public interface WriteTransaction {

    /**
     * Runs the work on the connection of the write transaction open on the current thread, inside that
     * transaction: the work neither commits nor closes it.
     *
     * @param <R> the result type
     * @param work the statements to run
     * @return the work's result, or empty when no write transaction is open on this thread
     */
    <R> Optional<R> join(Work<R> work);

    /**
     * Statements run on a connection the caller does not own.
     *
     * @param <R> the result type
     */
    @FunctionalInterface
    interface Work<R> {

        /**
         * Runs the statements.
         *
         * @param connection the connection of the open transaction
         * @return the result
         * @throws SQLException if a statement fails
         */
        R run(Connection connection) throws SQLException;
    }
}
