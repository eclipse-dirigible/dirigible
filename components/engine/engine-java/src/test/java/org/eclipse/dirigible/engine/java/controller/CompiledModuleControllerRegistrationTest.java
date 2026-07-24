/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.runtime.ClientBeansHolder;
import org.eclipse.dirigible.engine.java.runtime.ClientClassLoaderHolder;
import org.eclipse.dirigible.engine.java.runtime.JavaCompiledOutputDirectory;
import org.eclipse.dirigible.engine.java.runtime.JavaLoader;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceCompiler;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;

/**
 * End-to-end (within engine-java): a controller shipped by an AOT-packaged {@code compiled} module
 * - installed through {@code JavaLoader.installCompiledModules} rather than compiled from registry
 * sources - registers a real route in the {@link ControllerRouter} via the standard
 * {@link ControllerClassConsumer}. No runtime {@code javac} is involved.
 */
class CompiledModuleControllerRegistrationTest {

    private static final ApplicationEventPublisher NOOP_PUBLISHER = new ApplicationEventPublisher() {
        @Override
        public void publishEvent(ApplicationEvent event) {
            // intentionally empty
        }

        @Override
        public void publishEvent(Object event) {
            // intentionally empty
        }
    };

    @TempDir
    Path tempDir;

    @Test
    void compiled_module_controller_registers_a_route_end_to_end() {
        ControllerRouter router = new ControllerRouter();
        ComponentContainer container = new ComponentContainer(new ClientBeansHolder());
        ControllerClassConsumer controllerConsumer = new ControllerClassConsumer(router, Optional.empty(), container);

        JavaCompiledOutputDirectory outputDirectory = mock(JavaCompiledOutputDirectory.class);
        when(outputDirectory.get()).thenReturn(tempDir);
        JavaLoader loader = new JavaLoader(new JavaSourceCompiler(), new ClientClassLoaderHolder(), container, List.of(controllerConsumer),
                outputDirectory, NOOP_PUBLISHER);

        // A compiled-module controller already on the classpath (no registry .java, no javac).
        Class<?> type = CompiledSampleController.class;
        String project = "acme-mod";
        loader.installCompiledModules(List.of(new LoadedClass(project, type.getName(), type, type.getClassLoader())));

        // The route is resolvable through the router - base path is /<fqn-with-slashes>, suffix /ping.
        String base = "/" + type.getName()
                                .replace('.', '/');
        RouteMatch match = router.match(HttpMethod.GET, project, base + "/ping")
                                 .orElseThrow(() -> new AssertionError("compiled-module controller route not registered"));
        assertEquals(type.getName(), match.entry()
                                          .fqn());
        assertEquals(HttpMethod.GET, match.route()
                                          .httpMethod());
        assertTrue(router.match(HttpMethod.POST, project, base + "/ping")
                         .isEmpty(),
                "only the declared GET route matches");
    }
}
