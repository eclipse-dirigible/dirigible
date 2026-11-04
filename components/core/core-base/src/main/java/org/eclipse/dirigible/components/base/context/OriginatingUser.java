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
     * Binds the given user to the current thread until the returned binding is closed, which restores
     * whatever was bound before - use it in a try-with-resources, so a pooled thread never keeps it.
     *
     * <p>
     * A scope rather than a callback on purpose: a callback would be one more implementation of the
     * callable that {@code TenantContext.execute} takes, and static analysis that cannot tell those
     * implementations apart then reads every caller of {@code execute} - a GET among them - as reaching
     * the message sends and handler runs bound here.
     *
     * @param user the originating user; {@code null} or blank binds no user
     * @return the binding to close
     */
    public static Binding bind(String user) {
        Binding binding = new Binding(CURRENT.get());
        set(user);
        return binding;
    }

    /** A user bound by {@link #bind(String)}; closing it restores the user bound before. */
    public static final class Binding implements AutoCloseable {

        private final String previous;

        private Binding(String previous) {
            this.previous = previous;
        }

        /** Restores the user bound before this binding. */
        @Override
        public void close() {
            set(previous);
        }
    }

    private static void set(String user) {
        if (user == null || user.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(user);
        }
    }
}
