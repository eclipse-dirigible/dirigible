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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Singleton owner of the current {@link ClientClassLoader}.
 *
 * <p>
 * Writes (rebuild → swap) happen on the synchronization thread; reads happen on every HTTP
 * dispatch. {@link AtomicReference} gives lock-free reads + linearizable swaps. The previous loader
 * is intentionally not closed — it must remain reachable until any in-flight code that holds a
 * {@code Class} loaded from it returns. Once those references drop, GC reclaims it.
 *
 * <p>
 * Modules that cache anything derived from the client classloader can register a
 * {@link #addSwapListener(Runnable) swap listener} to be notified on every rebuild. This lets a
 * consumer that lives in another module react to a recompilation without depending on
 * {@code engine-java} (e.g. {@code engine-bpm-flowable} refreshing the classloader it hands to the
 * Flowable engine).
 */
@Component
public class ClientClassLoaderHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientClassLoaderHolder.class);

    private final AtomicReference<ClientClassLoader> ref = new AtomicReference<>();

    private final List<Runnable> swapListeners = new CopyOnWriteArrayList<>();

    /** The currently-active classloader, or {@code null} before the first rebuild. */
    public ClientClassLoader current() {
        return ref.get();
    }

    /** Replace the active classloader and notify the registered swap listeners. */
    public void swap(ClientClassLoader next) {
        ref.set(next);
        for (Runnable listener : swapListeners) {
            try {
                listener.run();
            } catch (RuntimeException ex) {
                LOGGER.error("Client classloader swap listener [{}] failed", listener, ex);
            }
        }
    }

    /**
     * Register a listener invoked (synchronously, on the swapping thread) after every
     * {@link #swap(ClientClassLoader)}. A throwing listener is logged and does not affect the swap or
     * the other listeners.
     *
     * @param listener the callback to run on each rebuild
     */
    public void addSwapListener(Runnable listener) {
        swapListeners.add(listener);
    }

}
