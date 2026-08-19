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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton owner of the current {@link ModulesClassLoader} - the swap-point for the maven
 * dependency layer, mirroring {@link ClientClassLoaderHolder} for client code.
 *
 * <p>
 * Reads happen on every classloading path that goes through a {@link ClientClassLoader} parent
 * chain; writes happen only in the dependency swap pipeline, one at a time. {@link AtomicReference}
 * gives lock-free reads and linearizable swaps. The retired loader is intentionally not closed -
 * in-flight requests may still hold classes loaded from it; once those references drop, GC reclaims
 * it. Retired generations are tracked with {@link WeakReference}s so monitoring can report how many
 * are still pinned by live references.
 *
 * <p>
 * Before the first {@link #swap(List)} the holder serves an empty generation over no JARs - plain
 * parent-first delegation to the application classloader, byte-for-byte the pre-dynamic behavior.
 */
@Component
public class ModulesClassLoaderHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModulesClassLoaderHolder.class);

    /** The current loader. */
    private final AtomicReference<ModulesClassLoader> ref = new AtomicReference<>();

    /** The generation counter - 0 until the first swap. */
    private final AtomicInteger generation = new AtomicInteger();

    /** Weak references to retired generations, for the monitoring counters. */
    private final List<WeakReference<ModulesClassLoader>> retired = new CopyOnWriteArrayList<>();

    /**
     * The currently-active modules classloader. Never null - before the first swap an empty generation
     * over the application classloader is created on demand.
     *
     * @return the current loader
     */
    public ModulesClassLoader current() {
        ModulesClassLoader current = ref.get();
        if (current != null) {
            return current;
        }
        ModulesClassLoader initial = new ModulesClassLoader(parentClassLoader(), List.of());
        return ref.compareAndSet(null, initial) ? initial : ref.get();
    }

    /**
     * Builds a fresh generation over the given JAR set and installs it. The previous generation is
     * retired (weakly tracked, never closed).
     *
     * @param jars the JAR paths of the new generation, in classpath order
     * @return the installed loader
     */
    public synchronized ModulesClassLoader swap(List<Path> jars) {
        ModulesClassLoader next = new ModulesClassLoader(parentClassLoader(), jars);
        ModulesClassLoader previous = ref.getAndSet(next);
        int number = generation.incrementAndGet();
        if (previous != null) {
            retired.add(new WeakReference<>(previous));
        }
        LOGGER.info("Modules classloader generation [{}] installed over [{}] jar(s)", number, jars.size());
        return next;
    }

    /**
     * The number of installed generations - 0 until the first swap.
     *
     * @return the generation number
     */
    public int generation() {
        return generation.get();
    }

    /**
     * How many retired generations are still pinned by live references - the heap-dump observability
     * counter. A steadily growing value indicates a loader leak in a consumer.
     *
     * @return the live retired-generation count
     */
    public int retiredGenerationsLive() {
        retired.removeIf(reference -> reference.get() == null);
        return retired.size();
    }

    /**
     * Parent class loader.
     *
     * @return the class loader
     */
    private static ClassLoader parentClassLoader() {
        return ModulesClassLoaderHolder.class.getClassLoader();
    }

}
