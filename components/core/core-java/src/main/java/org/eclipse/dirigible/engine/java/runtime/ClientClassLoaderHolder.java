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

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * Singleton owner of the current {@link ClientClassLoader}.
 *
 * <p>
 * Writes (rebuild → swap) happen on the synchronization thread; reads happen on every HTTP
 * dispatch. {@link AtomicReference} gives lock-free reads + linearizable swaps. The previous loader
 * is intentionally not closed — it must remain reachable until any in-flight code that holds a
 * {@code Class} loaded from it returns. Once those references drop, GC reclaims it.
 */
@Component
public class ClientClassLoaderHolder {

    private final AtomicReference<ClientClassLoader> ref = new AtomicReference<>();

    /** The currently-active classloader, or {@code null} before the first rebuild. */
    public ClientClassLoader current() {
        return ref.get();
    }

    /** Replace the active classloader. */
    public void swap(ClientClassLoader next) {
        ref.set(next);
    }

}
