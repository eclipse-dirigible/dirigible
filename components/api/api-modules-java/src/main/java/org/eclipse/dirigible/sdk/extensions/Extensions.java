/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.extensions;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.api.extensions.ExtensionsFacade;
import org.eclipse.dirigible.sdk.component.Beans;

/**
 * Discovers extensions contributed to an extension point. There is no dedicated annotation: an
 * <em>extension point</em> is simply a Java interface, and a <em>contribution</em> is a
 * {@link org.eclipse.dirigible.sdk.component.Component @Component} bean that implements it (its
 * {@code @Component} name is the contribution name). The preferred way to consume contributions is
 * <b>collection injection</b> — declare a {@code List<MyExtensionPoint>} constructor parameter;
 * reach for {@link #find(Class)} / {@link #findFirst(Class)} only outside an injection point (a
 * static context, a factory). Both return the same beans the container would inject.
 *
 * <p>
 * The legacy string-keyed lookup {@link #getExtensions(String)} remains for TypeScript / JavaScript
 * extension points (resolved through the platform's extensions registry).
 */
public final class Extensions {

    private Extensions() {}

    /**
     * Every contribution to {@code extensionPointType} — i.e. every {@code @Component} bean assignable
     * to it.
     *
     * @param <T> the extension point type
     * @param extensionPointType the extension point interface
     * @return the contributions, in registration order; empty if none
     */
    public static <T> List<T> find(Class<T> extensionPointType) {
        return Beans.getAll(extensionPointType);
    }

    /**
     * The first contribution to {@code extensionPointType}, or empty if none. Use only when the
     * extension point is single-valued by design.
     *
     * @param <T> the extension point type
     * @param extensionPointType the extension point interface
     * @return the first contribution, or empty
     */
    public static <T> Optional<T> findFirst(Class<T> extensionPointType) {
        List<T> all = find(extensionPointType);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /**
     * Legacy string-keyed lookup of contribution module names. Kept for TypeScript / JavaScript
     * extension points.
     *
     * @param extensionPointName the extension point name
     * @return the contributing module names
     * @throws Exception if the platform extensions registry cannot be queried
     */
    public static String[] getExtensions(String extensionPointName) throws Exception {
        return ExtensionsFacade.getExtensions(extensionPointName);
    }
}
