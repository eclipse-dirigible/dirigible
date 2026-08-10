/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.registry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * Tracks client requests that mutate the registry, so the synchronization reconciler can tell a
 * half-applied write from a finished one.
 *
 * <p>
 * A publish replaces a collection by DELETING it and copying it back milliseconds later, and both
 * steps can happen inside a single request. A reconciler that looks into that hole sees sources
 * that are about to reappear as deleted. It therefore asks here whether a mutation was in flight,
 * rather than inferring it from "the registry changed" - the file-system watcher reports a genuine
 * deletion exactly the same way, and treating the two alike would delay every deletion by a pass.
 */
@Component
public class RegistryMutationTracker {

    /** Registry-mutating requests currently being served. */
    private final AtomicInteger inFlight = new AtomicInteger();

    /** Registry-mutating requests served so far - lets an observer detect one that came and went. */
    private final AtomicLong completed = new AtomicLong();

    /** Marks the start of a registry-mutating request. */
    public void enter() {
        inFlight.incrementAndGet();
    }

    /** Marks the end of a registry-mutating request. */
    public void exit() {
        inFlight.decrementAndGet();
        completed.incrementAndGet();
    }

    /**
     * Whether a registry-mutating request is being served right now.
     *
     * @return true if at least one such request is in flight
     */
    public boolean isMutating() {
        return inFlight.get() > 0;
    }

    /**
     * The number of registry-mutating requests served so far. Compare a value taken earlier with the
     * current one to find out whether a mutation completed in between.
     *
     * @return the count of completed registry-mutating requests
     */
    public long completedMutations() {
        return completed.get();
    }

}
