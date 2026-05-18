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

import java.lang.reflect.Constructor;

import org.eclipse.dirigible.engine.java.annotations.Repository;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Built-in {@link JavaClassConsumer} that registers client classes annotated with
 * {@link Repository}. Instantiates each via its public no-arg constructor and stores the singleton
 * in {@link RepositoryRegistry}; {@code ControllerClassConsumer} (engine-java) then satisfies
 * {@code @Inject} field bindings through the registry's {@code DependencyResolver} surface.
 *
 * <p>
 * Repository instances live for the lifetime of the current {@code ClientClassLoader} generation.
 * When the loader is swapped on a hot-reload, {@code onClassUnloaded} drops the old entry before
 * the new generation registers the replacement.
 */
@Component
public class RepositoryClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryClassConsumer.class);

    private final RepositoryRegistry registry;

    @Autowired
    public RepositoryClassConsumer(RepositoryRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Repository.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        Class<?> type = info.type();
        try {
            Object instance = instantiate(type);
            registry.register(type, instance);
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register @Repository [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        registry.unregister(info.type());
    }

    private static Object instantiate(Class<?> type) {
        try {
            Constructor<?> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("@Repository [" + type.getName() + "] must have a public no-arg constructor: " + e.getMessage(),
                    e);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate @Repository [" + type.getName() + "]: " + e.getMessage(), e);
        }
    }
}
