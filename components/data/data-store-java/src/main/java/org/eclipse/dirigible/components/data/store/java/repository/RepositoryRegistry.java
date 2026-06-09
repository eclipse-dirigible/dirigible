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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.engine.java.spi.DependencyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Holds the singleton client repository instances and resolves them by type for the engine's
 * {@code @Inject} mechanism. Keyed by the repository's runtime class; lookups also accept any
 * superclass / interface the repository assigns to (e.g. a field typed as {@link JavaRepository}
 * resolves to the registered subclass if there's exactly one).
 *
 * <p>
 * Implements {@link DependencyResolver} so {@code ControllerClassConsumer} discovers it via Spring
 * auto-wiring without {@code data-store-java} having to know about the engine's controller stack.
 */
@Component
public class RepositoryRegistry implements DependencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryRegistry.class);

    private volatile Map<Class<?>, Object> repositories = new LinkedHashMap<>();

    private final Object writeLock = new Object();

    /**
     * Register a fresh instance under its runtime class. Replaces any prior entry.
     *
     * @param repoClass the runtime class to key the entry on
     * @param instance the repository singleton
     */
    public void register(Class<?> repoClass, Object instance) {
        synchronized (writeLock) {
            Map<Class<?>, Object> next = new LinkedHashMap<>(repositories);
            next.put(repoClass, instance);
            repositories = next;
            LOGGER.info("Registered repository [{}]", repoClass.getName());
        }
    }

    /**
     * Drop the entry for {@code repoClass} if present.
     *
     * @param repoClass the runtime class whose entry should be removed
     */
    public void unregister(Class<?> repoClass) {
        synchronized (writeLock) {
            Map<Class<?>, Object> next = new LinkedHashMap<>(repositories);
            if (next.remove(repoClass) != null) {
                repositories = next;
                LOGGER.info("Unregistered repository [{}]", repoClass.getName());
            }
        }
    }

    /**
     * @return the number of registered repositories — useful in tests
     */
    public int size() {
        return repositories.size();
    }

    @Override
    public Optional<Object> resolve(Class<?> type) {
        // Exact-class match first — covers the typical case where the field is declared as the
        // concrete repository class.
        Object exact = repositories.get(type);
        if (exact != null) {
            return Optional.of(exact);
        }
        // Fall back to "assignable from" scan for fields typed as the base JavaRepository<T> or an
        // interface the client introduced on top of a concrete repository.
        List<Object> assignable = repositories.entrySet()
                                              .stream()
                                              .filter(e -> type.isAssignableFrom(e.getKey()))
                                              .map(Map.Entry::getValue)
                                              .toList();
        if (assignable.size() == 1) {
            return Optional.of(assignable.get(0));
        }
        return Optional.empty();
    }
}
