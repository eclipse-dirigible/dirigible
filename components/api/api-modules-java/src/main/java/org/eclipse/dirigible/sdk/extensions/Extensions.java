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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.api.extensions.ExtensionsFacade;
import org.eclipse.dirigible.components.base.spring.BeanProvider;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers extensions contributed to an extension point. The typed entry points
 * {@link #find(Class)} and {@link #findFirst(Class)} return instantiated implementations cast to
 * the requested interface — no reflection needed at the call site.
 *
 * <p>
 * Resolution is dynamic: a new contribution becomes visible to the next call after its
 * synchronization cycle completes. Each call instantiates fresh instances via the impl class's
 * public no-arg constructor; cache them at the call site if you need a singleton.
 *
 * <p>
 * The legacy string-keyed lookup {@link #getExtensions(String)} remains for compatibility with
 * TypeScript / JavaScript extension points; Java code should prefer the typed variants.
 */
public final class Extensions {

    private static final Logger LOGGER = LoggerFactory.getLogger(Extensions.class);

    private Extensions() {}

    /**
     * Returns every registered implementation of the given extension point, instantiated via the impl's
     * public no-arg constructor. Implementations whose class can't be loaded or doesn't actually
     * implement {@code extensionPointType} are logged and skipped.
     */
    public static <T> List<T> find(Class<T> extensionPointType) throws Exception {
        String[] modules = ExtensionsFacade.getExtensions(extensionPointType.getName());
        if (modules == null || modules.length == 0) {
            return List.of();
        }
        ClassLoader clientLoader = clientClassLoader();
        List<T> result = new ArrayList<>(modules.length);
        for (String fqn : modules) {
            T instance = instantiate(extensionPointType, fqn, clientLoader);
            if (instance != null) {
                result.add(instance);
            }
        }
        return result;
    }

    /**
     * Returns the first registered implementation of the given extension point, or empty if none are
     * registered. Ordering across implementations is not guaranteed — use this only when the extension
     * point is single-valued by design.
     */
    public static <T> Optional<T> findFirst(Class<T> extensionPointType) throws Exception {
        List<T> all = find(extensionPointType);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(0));
    }

    /** Legacy string-keyed lookup. Kept for TS/JS interop and existing callers. */
    public static String[] getExtensions(String extensionPointName) throws Exception {
        return ExtensionsFacade.getExtensions(extensionPointName);
    }

    private static <T> T instantiate(Class<T> extensionPointType, String implFqn, ClassLoader clientLoader) {
        try {
            Class<?> implClass = Class.forName(implFqn, true, clientLoader);
            if (!extensionPointType.isAssignableFrom(implClass)) {
                LOGGER.warn("Skipping extension [{}]: does not implement extension point [{}].", implFqn, extensionPointType.getName());
                return null;
            }
            Object instance = implClass.getDeclaredConstructor()
                                       .newInstance();
            return extensionPointType.cast(instance);
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate extension [{}] for extension point [{}]: {}", implFqn, extensionPointType.getName(),
                    e.getMessage(), e);
            return null;
        }
    }

    private static ClassLoader clientClassLoader() {
        ClientClassLoaderHolder holder = BeanProvider.getBean(ClientClassLoaderHolder.class);
        ClassLoader cl = holder.current();
        return cl != null ? cl
                : Thread.currentThread()
                        .getContextClassLoader();
    }
}
