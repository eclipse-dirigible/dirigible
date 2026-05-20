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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class RepositoryRegistryTest {

    @Test
    void resolve_returns_exact_match() {
        RepositoryRegistry registry = new RepositoryRegistry();
        FooRepo foo = new FooRepo();
        registry.register(FooRepo.class, foo);

        assertSame(foo, registry.resolve(FooRepo.class)
                                .orElseThrow());
    }

    @Test
    void resolve_unknown_type_is_empty() {
        RepositoryRegistry registry = new RepositoryRegistry();
        assertTrue(registry.resolve(FooRepo.class)
                           .isEmpty());
    }

    @Test
    void resolve_via_superclass_when_unique() {
        RepositoryRegistry registry = new RepositoryRegistry();
        FooRepo foo = new FooRepo();
        registry.register(FooRepo.class, foo);

        // Field declared with the abstract type still resolves to the single concrete repository.
        Optional<Object> resolved = registry.resolve(AbstractRepo.class);
        assertSame(foo, resolved.orElseThrow());
    }

    @Test
    void resolve_via_superclass_is_empty_when_multiple_candidates() {
        RepositoryRegistry registry = new RepositoryRegistry();
        registry.register(FooRepo.class, new FooRepo());
        registry.register(BarRepo.class, new BarRepo());

        // Two concrete repos both extend AbstractRepo — ambiguous, fall back to empty rather than
        // pick a random one.
        assertTrue(registry.resolve(AbstractRepo.class)
                           .isEmpty());
    }

    @Test
    void unregister_drops_entry() {
        RepositoryRegistry registry = new RepositoryRegistry();
        registry.register(FooRepo.class, new FooRepo());
        assertEquals(1, registry.size());

        registry.unregister(FooRepo.class);
        assertEquals(0, registry.size());
        assertTrue(registry.resolve(FooRepo.class)
                           .isEmpty());
    }

    @Test
    void register_replaces_existing_entry() {
        RepositoryRegistry registry = new RepositoryRegistry();
        FooRepo first = new FooRepo();
        FooRepo second = new FooRepo();
        registry.register(FooRepo.class, first);
        registry.register(FooRepo.class, second);

        assertEquals(1, registry.size());
        assertSame(second, registry.resolve(FooRepo.class)
                                   .orElseThrow());
    }

    // --- fixtures --------------------------------------------------------------------------------

    abstract static class AbstractRepo {
    }

    static class FooRepo extends AbstractRepo {
    }

    static class BarRepo extends AbstractRepo {
    }
}
