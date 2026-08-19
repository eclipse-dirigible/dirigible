/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.dependencies;

import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Published after the maven dependency layer swapped to a new modules-classloader generation.
 * Listeners react to the change - the Java engine rediscovers AOT compiled modules, invalidates its
 * compile classpath and rebuilds the client sources; the monitoring surface records the change.
 * Published synchronously on the swapping thread, so a listener's reaction completes before the
 * swap pipeline reports success.
 */
public class DependenciesChangedEvent extends ApplicationEvent {

    /** The added coordinates. */
    private final Set<String> added;

    /** The removed coordinates. */
    private final Set<String> removed;

    /** The mediated versions, groupId:artifactId to the chosen version. */
    private final Map<String, String> mediated;

    /** The installed modules-classloader generation number. */
    private final int generation;

    /**
     * Instantiates a new event.
     *
     * @param source the publisher
     * @param added the added coordinates
     * @param removed the removed coordinates
     * @param mediated the mediated versions
     * @param generation the installed generation number
     */
    public DependenciesChangedEvent(Object source, Set<String> added, Set<String> removed, Map<String, String> mediated, int generation) {
        super(source);
        this.added = Collections.unmodifiableSet(new LinkedHashSet<>(added));
        this.removed = Collections.unmodifiableSet(new LinkedHashSet<>(removed));
        this.mediated = Collections.unmodifiableMap(new LinkedHashMap<>(mediated));
        this.generation = generation;
    }

    /**
     * Gets the added coordinates.
     *
     * @return the added coordinates
     */
    public Set<String> getAdded() {
        return added;
    }

    /**
     * Gets the removed coordinates.
     *
     * @return the removed coordinates
     */
    public Set<String> getRemoved() {
        return removed;
    }

    /**
     * Gets the mediated versions.
     *
     * @return the mediated versions, groupId:artifactId to the chosen version
     */
    public Map<String, String> getMediated() {
        return mediated;
    }

    /**
     * Gets the installed modules-classloader generation number.
     *
     * @return the generation number
     */
    public int getGeneration() {
        return generation;
    }

}
