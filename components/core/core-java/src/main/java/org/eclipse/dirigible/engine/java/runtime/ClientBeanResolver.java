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
import java.util.Optional;

/**
 * Read view of the client bean container for the current {@code ClientClassLoader} generation.
 *
 * <p>
 * Implemented by the engine's component container and published through {@link ClientBeansHolder}.
 * It lives in {@code core-java} (not {@code engine-java}) so the SDK facade
 * {@code org.eclipse.dirigible.sdk.component.Beans} can reach client beans without dragging
 * {@code engine-java} onto the SDK's compile classpath — the same module-cycle avoidance that put
 * {@link ClientClassLoader} here.
 */
public interface ClientBeanResolver {

    /**
     * Resolve the single client bean assignable to {@code type}.
     *
     * @param <T> the bean type
     * @param type the required type (class or interface)
     * @return the matching bean, or empty if none (or more than one ambiguous candidate) is registered
     */
    <T> Optional<T> get(Class<T> type);

    /**
     * Resolve a client bean by its registered name, checked to be assignable to {@code type}.
     *
     * @param <T> the bean type
     * @param name the bean name (default = decapitalized simple class name, or the {@code @Component}
     *        value)
     * @param type the required type
     * @return the matching bean, or empty if no bean with that name assignable to {@code type} exists
     */
    <T> Optional<T> get(String name, Class<T> type);

    /**
     * Resolve every client bean assignable to {@code type}, in registration order.
     *
     * @param <T> the bean type
     * @param type the required type (typically an interface / extension point)
     * @return all matching beans; empty if none
     */
    <T> List<T> getAll(Class<T> type);

}
