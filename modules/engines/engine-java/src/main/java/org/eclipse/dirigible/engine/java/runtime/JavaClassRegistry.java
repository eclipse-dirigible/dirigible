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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dirigible.engine.java.handler.JavaHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-process registry of loaded {@link JavaHandler} classes keyed by
 * {@code <project>::<class-fqn>}.
 *
 * <p>
 * The {@link org.eclipse.dirigible.engine.java.synchronizer.JavaSynchronizer JavaSynchronizer}
 * is the only writer. The {@link org.eclipse.dirigible.engine.java.endpoint.JavaEndpoint
 * JavaEndpoint} is the only reader. A {@link ConcurrentHashMap} is sufficient: each entry's
 * value is a {@link LoadedHandler} that is itself immutable, and replacement on update is a
 * single {@code put} which is atomic.
 *
 * <p>
 * On {@code register}, an existing entry is overwritten and the previous {@link LoadedHandler}'s
 * {@link ClassLoader} becomes unreachable as soon as no in-flight request still holds it. JVM
 * unloads the class metadata at the next GC. There is no explicit {@code close()} step — the
 * absence of references is the contract.
 */
@Component
public class JavaClassRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaClassRegistry.class);

    private final Map<String, LoadedHandler> handlers = new ConcurrentHashMap<>();

    /** Compose the registry key from a project name and an FQN. */
    public static String key(String project, String classFqn) {
        return project + "::" + classFqn;
    }

    public void register(LoadedHandler handler) {
        String k = key(handler.getProject(), handler.getClassFqn());
        LoadedHandler previous = handlers.put(k, handler);
        if (previous != null) {
            LOGGER.debug("Replaced handler [{}]; previous ClassLoader is now unreachable and will be GC'd", k);
        } else {
            LOGGER.debug("Registered handler [{}]", k);
        }
    }

    public Optional<LoadedHandler> find(String project, String classFqn) {
        return Optional.ofNullable(handlers.get(key(project, classFqn)));
    }

    public boolean unregister(String project, String classFqn) {
        String k = key(project, classFqn);
        LoadedHandler removed = handlers.remove(k);
        if (removed != null) {
            LOGGER.debug("Unregistered handler [{}]; ClassLoader is now unreachable", k);
            return true;
        }
        return false;
    }

    /** Number of currently registered handlers — primarily for test/observability. */
    public int size() {
        return handlers.size();
    }

}
