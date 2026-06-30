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
 * Singleton owner of the current {@link ClientBeanResolver}.
 *
 * <p>
 * Mirrors {@link ClientClassLoaderHolder}: the engine's component container publishes itself here
 * on every rebuild (synchronization thread); the SDK facade
 * {@code org.eclipse.dirigible.sdk.component.Beans} reads it on every client call.
 * {@link AtomicReference} gives lock-free reads and linearizable swaps.
 */
@Component
public class ClientBeansHolder {

    private final AtomicReference<ClientBeanResolver> ref = new AtomicReference<>();

    /** The currently-active client bean resolver, or {@code null} before the first rebuild. */
    public ClientBeanResolver current() {
        return ref.get();
    }

    /** Replace the active client bean resolver. */
    public void swap(ClientBeanResolver next) {
        ref.set(next);
    }

}
