/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.context;

import java.util.Optional;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;

/**
 * The user whose action raised the event being handled on the current thread.
 *
 * <p>
 * An event a write records in the outbox is delivered, and handled, on a thread that serves no
 * request, so nothing there says who caused it - and every row a posting, a reaction or a roll-up
 * wrote in reaction was attributed to the {@code guest} fallback (#8228). The platform carries that
 * user with the event instead (the outbox row, then a property of the message the platform's own
 * producer stamps) and binds it here for the duration of the delivery and of the handler, so the
 * audit columns of the derived rows name the user whose write raised the event.
 *
 * <p>
 * It is an attribution, never an authentication: it is read after the request's own user and grants
 * no role. It is bound only by the platform from a record it wrote itself - an outbox row, or a
 * message property its producer stamped - and never from anything a client sends.
 */
public final class OriginatingUser {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private OriginatingUser() {}

    /**
     * The user bound to the current thread.
     *
     * @return the originating user, or empty when the thread is not handling an event raised by a known
     *         user
     */
    public static Optional<String> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Runs the callable with the given user bound to the current thread, and restores whatever was
     * bound before once it returns or throws - so a pooled thread never keeps it.
     *
     * @param user the originating user; {@code null} or blank binds no user for the call
     * @param callable the work to run
     * @param <R> the result type
     * @param <E> the exception type
     * @return the callable's result
     * @throws E the callable's exception
     */
    public static <R, E extends Throwable> R callAs(String user, CallableResultAndException<R, E> callable) throws E {
        String previous = CURRENT.get();
        bind(user);
        try {
            return callable.call();
        } finally {
            bind(previous);
        }
    }

    private static void bind(String user) {
        if (user == null || user.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(user);
        }
    }
}
