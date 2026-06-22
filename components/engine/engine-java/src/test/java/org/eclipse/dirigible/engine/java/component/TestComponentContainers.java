/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.component;

import java.util.Arrays;
import java.util.List;

import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;

/**
 * Test helper that builds a {@link ComponentContainer} populated with the given client classes, the
 * way {@code JavaLoader} would for one generation. Lets consumer tests obtain ready, injected bean
 * instances without standing up the whole engine.
 */
public final class TestComponentContainers {

    private TestComponentContainers() {}

    /**
     * @param classes the client classes of the generation (beans are filtered internally)
     * @return a container with those classes' beans instantiated
     */
    public static ComponentContainer of(Class<?>... classes) {
        ComponentContainer container = new ComponentContainer(new ClientBeansHolder());
        List<LoadedClass> loaded = Arrays.stream(classes)
                                         .map(type -> new LoadedClass("p", type.getName(), type, type.getClassLoader()))
                                         .toList();
        container.rebuild(loaded);
        return container;
    }
}
