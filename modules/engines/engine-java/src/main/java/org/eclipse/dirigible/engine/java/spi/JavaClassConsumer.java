/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.spi;

/**
 * Extension point implemented by modules that wish to react to client {@code .java} classes once
 * they have been compiled and loaded by {@code engine-java}.
 *
 * <p>
 * All beans implementing this interface are auto-discovered via Spring and offered every loaded
 * class on each rebuild cycle. A consumer that returns {@code true} from {@link #accepts(Class)}
 * will receive a paired {@link #onClassLoaded} / {@link #onClassUnloaded} for the class.
 *
 * <p>
 * Built-in consumers:
 * <ul>
 * <li>{@code HandlerClassConsumer} — claims classes that implement
 * {@code org.eclipse.dirigible.engine.java.handler.JavaHandler} and exposes them as REST endpoints
 * via {@code JavaEndpoint}.</li>
 * </ul>
 *
 * <p>
 * The {@code data-store-java} module adds an {@code EntityClassConsumer} that claims classes
 * annotated with {@code @Entity}.
 */
public interface JavaClassConsumer {

    /**
     * Decide whether this consumer wants to handle the given class. Called once per class per rebuild;
     * cheap by convention (annotation or interface check).
     */
    boolean accepts(Class<?> clazz);

    /**
     * Called when a class accepted by this consumer enters the registry (either newly compiled or
     * replacing a previous definition).
     */
    void onClassLoaded(LoadedClass info);

    /**
     * Called when a class accepted by this consumer leaves the registry (source removed or superseded
     * by a rebuild that no longer produces it).
     */
    void onClassUnloaded(LoadedClass info);

}
