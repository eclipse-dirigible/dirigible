/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.handler;

import org.eclipse.dirigible.engine.java.runtime.JavaClassRegistry;
import org.eclipse.dirigible.engine.java.runtime.LoadedHandler;
import org.eclipse.dirigible.engine.java.spi.JavaClassConsumer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Built-in {@link JavaClassConsumer} that exposes client classes implementing {@link JavaHandler}
 * as REST endpoints via {@link org.eclipse.dirigible.engine.java.endpoint.JavaEndpoint
 * JavaEndpoint}.
 *
 * <p>
 * The actual registry is {@link JavaClassRegistry}; this consumer is the bridge that translates the
 * engine-wide SPI events into registry register/unregister calls.
 */
@Component
public class HandlerClassConsumer implements JavaClassConsumer {

    private final JavaClassRegistry registry;

    @Autowired
    public HandlerClassConsumer(JavaClassRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean accepts(Class<?> clazz) {
        return JavaHandler.class.isAssignableFrom(clazz);
    }

    @Override
    public void onClassLoaded(LoadedClass info) {
        @SuppressWarnings("unchecked")
        Class<? extends JavaHandler> handlerClass = (Class<? extends JavaHandler>) info.type();
        registry.register(new LoadedHandler(info.project(), info.fqn(), info.loader(), handlerClass));
    }

    @Override
    public void onClassUnloaded(LoadedClass info) {
        registry.unregister(info.project(), info.fqn());
    }

}
