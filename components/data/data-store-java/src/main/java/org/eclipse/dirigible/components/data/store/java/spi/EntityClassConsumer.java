/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.java.spi;

import org.eclipse.dirigible.components.data.store.java.manager.JavaEntityManager;
import org.eclipse.dirigible.engine.java.annotations.Entity;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Plugs the {@code data-store-java} runtime into {@code engine-java}'s class-loaded events: every
 * class annotated with {@link Entity} is registered with the {@link JavaEntityManager} (and the
 * Hibernate {@code SessionFactory} is rebuilt to include it).
 */
@Component
@Order(100) // Run first so entity tables exist before any repository / controller binds to them.
public class EntityClassConsumer implements JavaClassConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityClassConsumer.class);

    private final JavaEntityManager entityManager;

    @Autowired
    public EntityClassConsumer(JavaEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return clazz.isAnnotationPresent(Entity.class);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        String key = key(info);
        try {
            entityManager.register(key, info.type());
        } catch (RuntimeException e) {
            LOGGER.error("Failed to register Java entity [{}]: {}", info.fqn(), e.getMessage(), e);
        }
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        entityManager.unregister(key(info));
    }

    private static String key(LoadedClass info) {
        return info.project() + "::" + info.fqn();
    }

}
