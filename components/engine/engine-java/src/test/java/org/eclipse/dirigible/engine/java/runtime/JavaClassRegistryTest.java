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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class JavaClassRegistryTest {

    @Test
    void register_replaces_existing_entry() {
        JavaClassRegistry registry = new JavaClassRegistry();
        LoadedHandler first = handler();
        LoadedHandler second = handler();

        registry.register(first);
        registry.register(second);

        assertEquals(1, registry.size());
        assertSame(second, registry.find(second.getProject(), second.getClassFqn())
                                   .orElseThrow());
    }

    @Test
    void unregister_returns_false_for_absent_entry() {
        JavaClassRegistry registry = new JavaClassRegistry();
        assertFalse(registry.unregister("p", "c"));
    }

    @Test
    void unregister_drops_existing_entry() {
        JavaClassRegistry registry = new JavaClassRegistry();
        LoadedHandler h = handler();
        registry.register(h);
        assertTrue(registry.unregister(h.getProject(), h.getClassFqn()));
        assertEquals(0, registry.size());
    }

    private static LoadedHandler handler() {
        return new LoadedHandler("p", "c.H", JavaClassRegistryTest.class.getClassLoader(), Stub.class);
    }

    public static final class Stub implements JavaHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) {}
    }

}
